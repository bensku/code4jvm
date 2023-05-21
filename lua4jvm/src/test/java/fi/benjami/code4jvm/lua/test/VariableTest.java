package fi.benjami.code4jvm.lua.test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import fi.benjami.code4jvm.lua.ir.LuaBlock;
import fi.benjami.code4jvm.lua.ir.LuaLocalVar;
import fi.benjami.code4jvm.lua.ir.LuaType;
import fi.benjami.code4jvm.lua.ir.expr.ArithmeticExpr;
import fi.benjami.code4jvm.lua.ir.expr.FunctionCallExpr;
import fi.benjami.code4jvm.lua.ir.expr.LuaConstant;
import fi.benjami.code4jvm.lua.ir.expr.VariableExpr;
import fi.benjami.code4jvm.lua.ir.stmt.ReturnStmt;
import fi.benjami.code4jvm.lua.ir.stmt.SetVariablesStmt;
import fi.benjami.code4jvm.lua.runtime.LuaFunction;

public class VariableTest {

	@Test
	public void simpleVariables() throws Throwable {
		// A function that computes sum of its arguments
		var a = new LuaLocalVar("a");
		var b = new LuaLocalVar("b");
		var c = new LuaLocalVar("c");
		var type = LuaType.function(
				List.of(), 
				List.of(a, b),
				new LuaBlock(List.of(
						new SetVariablesStmt(
								List.of(c),
								List.of(new ArithmeticExpr(new VariableExpr(a), ArithmeticExpr.Kind.ADD, new VariableExpr(b))),
								false
								),
						new ReturnStmt(List.of(new VariableExpr(c)))
				)));
		var func = new LuaFunction(type, new Object[0]);
		assertEquals(3d, func.call(1d, 2d));
		assertThrows(UnsupportedOperationException.class, () -> func.call(null, 4d));
	}
	
	@Test
	public void unknownTypes() throws Throwable {
		// Variable is set multiple times, leading to unknown type
		var a = new LuaLocalVar("a");
		var b = new LuaLocalVar("b");
		var c = new LuaLocalVar("c");
		var type = LuaType.function(
				List.of(), 
				List.of(a, b),
				new LuaBlock(List.of(
						new SetVariablesStmt(
								List.of(c),
								List.of(new VariableExpr(a)),
								false
								),
						new SetVariablesStmt(
								List.of(c),
								List.of(new VariableExpr(b)),
								false
								),
						new ReturnStmt(List.of(new VariableExpr(c)))
				)));
		var func = new LuaFunction(type, new Object[0]);
		assertEquals(10d, func.call(5d, 10d));
		assertNull(func.call(1d, null));
	}
	
	@Test
	public void multipleVariables() throws Throwable {
		var a = new LuaLocalVar("a");
		var b = new LuaLocalVar("b");
		var c = new LuaLocalVar("c");
		var d = new LuaLocalVar("d");
		var type = LuaType.function(
				List.of(), 
				List.of(a, b),
				new LuaBlock(List.of(
						new SetVariablesStmt(
								List.of(c, d),
								List.of(new VariableExpr(a), new VariableExpr(b)),
								false
								),
						new ReturnStmt(List.of(new VariableExpr(c), new VariableExpr(d)))
				)));
		var func = new LuaFunction(type, new Object[0]);
		assertArrayEquals(new Object[] {5d, 10d}, (Object[]) func.call(5d, 10d));
		assertArrayEquals(new Object[] {5d, null}, (Object[]) func.call(5d, null));
		assertArrayEquals(new Object[] {null, 2d}, (Object[]) func.call(null, 2d));
	}
	
	@Test
	public void spreadTuple() throws Throwable {
		var calledType = LuaType.function(
				List.of(),
				List.of(),
				new LuaBlock(List.of(
						new ReturnStmt(List.of(new LuaConstant(5d, LuaType.NUMBER), new LuaConstant(6d, LuaType.NUMBER)))
				)));
		var calledFunc = new LuaFunction(calledType, new Object[0]);
		
		{
			// Tuple known compile-time
			var a = new LuaLocalVar("a");
			var b = new LuaLocalVar("b");
			var c = new LuaLocalVar("c");
			var type = LuaType.function(
					List.of(),
					List.of(),
					new LuaBlock(List.of(
							new SetVariablesStmt(
									List.of(a, b, c),
									List.of(new FunctionCallExpr(new LuaConstant(calledFunc, calledType), List.of())),
									true),
							new ReturnStmt(List.of(new VariableExpr(a), new VariableExpr(b), new VariableExpr(c)))
							))
					);
			var func = new LuaFunction(type, new Object[0]);
			assertArrayEquals(new Object[] {5d, 6d, null}, (Object[]) func.call());
		}
		{
			// Tuple discovered runtime
			var a = new LuaLocalVar("a");
			var b = new LuaLocalVar("b");
			var c = new LuaLocalVar("c");
			var type = LuaType.function(
					List.of(),
					List.of(),
					new LuaBlock(List.of(
							new SetVariablesStmt(
									List.of(a, b, c),
									List.of(new FunctionCallExpr(new LuaConstant(calledFunc, LuaType.UNKNOWN), List.of())),
									true),
							new ReturnStmt(List.of(new VariableExpr(a), new VariableExpr(b), new VariableExpr(c)))
							))
					);
			var func = new LuaFunction(type, new Object[0]);
			assertArrayEquals(new Object[] {5d, 6d, null}, (Object[]) func.call());
		}
	}
}
