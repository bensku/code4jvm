package fi.benjami.code4jvm.lua.test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import fi.benjami.code4jvm.lua.ir.IrNode;
import fi.benjami.code4jvm.lua.ir.LuaBlock;
import fi.benjami.code4jvm.lua.ir.LuaLocalVar;
import fi.benjami.code4jvm.lua.ir.LuaType;
import fi.benjami.code4jvm.lua.ir.LuaVariable;
import fi.benjami.code4jvm.lua.ir.UpvalueTemplate;
import fi.benjami.code4jvm.lua.ir.expr.ArithmeticExpr;
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
				new UpvalueTemplate[0], 
				new LuaLocalVar[] {a, b},
				new LuaBlock(List.of(
						new SetVariablesStmt(
								new LuaVariable[] {c},
								new IrNode[] {new ArithmeticExpr(new VariableExpr(a), ArithmeticExpr.Kind.ADD, new VariableExpr(b))},
								false
								),
						new ReturnStmt(new IrNode[] {new VariableExpr(c)})
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
				new UpvalueTemplate[0], 
				new LuaLocalVar[] {a, b},
				new LuaBlock(List.of(
						new SetVariablesStmt(
								new LuaVariable[] {c},
								new IrNode[] {new VariableExpr(a)},
								false
								),
						new SetVariablesStmt(
								new LuaVariable[] {c},
								new IrNode[] {new VariableExpr(b)},
								false
								),
						new ReturnStmt(new IrNode[] {new VariableExpr(c)})
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
				new UpvalueTemplate[0], 
				new LuaLocalVar[] {a, b},
				new LuaBlock(List.of(
						new SetVariablesStmt(
								new LuaVariable[] {c, d},
								new IrNode[] {new VariableExpr(a), new VariableExpr(b)},
								false
								),
						new ReturnStmt(new IrNode[] {new VariableExpr(c), new VariableExpr(d)})
				)));
		var func = new LuaFunction(type, new Object[0]);
		assertArrayEquals(new Object[] {5d, 10d}, (Object[]) func.call(5d, 10d));
		assertArrayEquals(new Object[] {5d, null}, (Object[]) func.call(5d, null));
		assertArrayEquals(new Object[] {null, 2d}, (Object[]) func.call(null, 2d));
	}
}
