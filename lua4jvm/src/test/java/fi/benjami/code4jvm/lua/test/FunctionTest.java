package fi.benjami.code4jvm.lua.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import fi.benjami.code4jvm.lua.LuaVm;
import fi.benjami.code4jvm.lua.ir.LuaBlock;
import fi.benjami.code4jvm.lua.ir.LuaLocalVar;
import fi.benjami.code4jvm.lua.ir.LuaType;
import fi.benjami.code4jvm.lua.ir.UpvalueTemplate;
import fi.benjami.code4jvm.lua.ir.expr.ArithmeticExpr;
import fi.benjami.code4jvm.lua.ir.expr.LuaConstant;
import fi.benjami.code4jvm.lua.ir.expr.FunctionCallExpr;
import fi.benjami.code4jvm.lua.ir.expr.FunctionDeclExpr;
import fi.benjami.code4jvm.lua.ir.expr.VariableExpr;
import fi.benjami.code4jvm.lua.ir.stmt.ReturnStmt;
import fi.benjami.code4jvm.lua.runtime.LuaFunction;
import fi.benjami.code4jvm.lua.stdlib.LuaException;

public class FunctionTest {

	private final LuaVm dummy = new LuaVm();
	
	@Test
	public void emptyFunction() throws Throwable {
		// Empty function with no upvalues or arguments that returns nothing
		var type = LuaType.function(List.of(), List.of(), new LuaBlock(List.of(
				new ReturnStmt(List.of())
				)), "unknown", "main");
		var func = new LuaFunction(dummy, type, new Object[0]);
		func.call();
	}
	
	@Test
	public void simpleMath() throws Throwable {
		// A function that computes sum of its arguments
		var a = new LuaLocalVar("a");
		var b = new LuaLocalVar("b");
		var type = LuaType.function(
				List.of(), 
				List.of(a, b),
				new LuaBlock(List.of(new ReturnStmt(List.of(
						new ArithmeticExpr(new VariableExpr(a), ArithmeticExpr.Kind.ADD, new VariableExpr(b))
						))
				)), "unknown", "main");
		var func = new LuaFunction(dummy, type, new Object[0]);
		assertEquals(3d, func.call(1d, 2d));
		assertThrows(LuaException.class, () -> func.call(null, 4d));
	}
	
	@Test
	public void upvalues() throws Throwable {
		// A function that computes sum of an upvalue and an argument
		var a = new LuaLocalVar("a");
		var b = new LuaLocalVar("b");
		var type = LuaType.function(
				List.of(new UpvalueTemplate(a, LuaType.FLOAT, false)),
				List.of(b),
				new LuaBlock(List.of(new ReturnStmt(List.of(
						new ArithmeticExpr(new VariableExpr(a), ArithmeticExpr.Kind.ADD, new VariableExpr(b))
						))
				)), "unknown", "main");
		var func = new LuaFunction(dummy, type, new Object[] {10d});
		assertEquals(12d, func.call(2d));
	}
	
	@Test
	public void callFromLua() throws Throwable {
		// A function that calls another Lua function
		var a1 = new LuaLocalVar("a");
		var b1 = new LuaLocalVar("b");
		var targetType = LuaType.function(
				List.of(), 
				List.of(a1, b1),
				new LuaBlock(List.of(new ReturnStmt(List.of(
						new ArithmeticExpr(new VariableExpr(a1), ArithmeticExpr.Kind.ADD, new VariableExpr(b1))
						))
				)), "unknown", "main");
		var target = new LuaFunction(dummy, targetType, new Object[0]);
		
		var a2 = new LuaLocalVar("a");
		var b2 = new LuaLocalVar("b");
		var wrapperType = LuaType.function(
				List.of(), 
				List.of(a2, b2),
				new LuaBlock(List.of(
						new ReturnStmt(List.of(
								new FunctionCallExpr(
										new LuaConstant(target, targetType),
										List.of(new VariableExpr(a2), new VariableExpr(b2))
								)))
				)), "unknown", "main");
		var wrapper = new LuaFunction(dummy, wrapperType, new Object[0]);
		assertEquals(40d, wrapper.call(15d, 25d));
	}
	
	@Test
	@Disabled("something fishy with upvalues")
	public void declareFunction() throws Throwable {
		// A function that declares and returns another function (that we then call)
		var a = new LuaLocalVar("a");
		var b = new LuaLocalVar("b");
		var insideA = new LuaLocalVar("a");
		var insideB = new LuaLocalVar("b");
		var c = new LuaLocalVar("c");
		var type = LuaType.function(
				List.of(new UpvalueTemplate(a, LuaType.FLOAT, false)), 
				List.of(b),
				new LuaBlock(List.of(
						new ReturnStmt(List.of(new FunctionDeclExpr(
								"unknown", "main",
								List.of(a, b),
								List.of(c),
								new LuaBlock(List.of(
										new ReturnStmt(List.of(
												new ArithmeticExpr(
														new VariableExpr(insideA),
														ArithmeticExpr.Kind.ADD,
														new ArithmeticExpr(
																new VariableExpr(insideB),
																ArithmeticExpr.Kind.MULTIPLY,
																new VariableExpr(c)
																)
														)))
										))
								)))
				)), "unknown", "main");
		var func = new LuaFunction(dummy, type, new Object[] {10d});
		var nested = (LuaFunction) func.call(5d);
		assertEquals(27.5d, nested.call(3.5d));
		assertThrows(LuaException.class, () -> nested.call(new Object()));
	}
}
