package fi.benjami.code4jvm.lua.runtime;

import java.util.Arrays;

import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.call.CallTarget;

public class LuaOps {
	
	// TODO get rid of all of this; instead, use invokedynamic!
	
	public static final CallTarget ADD = target("add", 2);

	public static Object add(Object lhs, Object rhs) {
		if (lhs instanceof Double num1 && rhs instanceof Double num2) {
			return num1 + num2;
		} else {
			return resolveMetatable(lhs, Metamethod.ADD, rhs);
		}
	}
	
	public static final CallTarget SUBTRACT = target("subtract", 2);
	
	public static Object subtract(Object lhs, Object rhs) {
		if (lhs instanceof Double num1 && rhs instanceof Double num2) {
			return num1 - num2;
		} else {
			return resolveMetatable(lhs, Metamethod.SUBTRACT, rhs);
		}
	}
	
	public static final CallTarget MULTIPLY = target("multiply", 2);
	
	public static Object multiply(Object lhs, Object rhs) {
		if (lhs instanceof Double num1 && rhs instanceof Double num2) {
			return num1 * num2;
		} else {
			return resolveMetatable(lhs, Metamethod.MULTIPLY, rhs);
		}
	}
	
	public static final CallTarget DIVIDE = target("divide", 2);
	
	public static Object divide(Object lhs, Object rhs) {
		if (lhs instanceof Double num1 && rhs instanceof Double num2) {
			return num1 / num2;
		} else {
			return resolveMetatable(lhs, Metamethod.DIVIDE, rhs);
		}
	}
	
	public static final CallTarget POWER = target("power", 2);
	
	public static Object power(Object lhs, Object rhs) {
		if (lhs instanceof Double num1 && rhs instanceof Double num2) {
			return Math.pow(num1, num2);
		} else {
			return resolveMetatable(lhs, Metamethod.POWER, rhs);
		}
	}
	
	public static final CallTarget MODULO = target("modulo", 2);
	
	public static Object modulo(Object lhs, Object rhs) {
		if (lhs instanceof Double num1 && rhs instanceof Double num2) {
			return num1 % num2;
		} else {
			return resolveMetatable(lhs, Metamethod.MODULO, rhs);
		}
	}
		
	private static CallTarget target(String name, int argCount) {
		var args = new Type[argCount];
		Arrays.fill(args, Type.OBJECT);
		return CallTarget.staticMethod(Type.of(LuaOps.class), Type.OBJECT, name, args);
	}
	
	private static Object resolveMetatable(Object lhs, Metamethod method, Object rhs) {
		throw new UnsupportedOperationException("operation " + method + " is not supported for " + lhs + " and " + rhs);
	}
}
