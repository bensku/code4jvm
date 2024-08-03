package fi.benjami.code4jvm.lua.runtime;

import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.call.CallTarget;
import fi.benjami.code4jvm.lua.ir.IrNode;
import fi.benjami.code4jvm.lua.ir.LuaLocalVar;
import fi.benjami.code4jvm.lua.ir.expr.FunctionCallExpr;
import fi.benjami.code4jvm.lua.ir.expr.VariableExpr;

/**
 * Utility methods for working with Lua multivals. Many of these are called
 * very frequently from the generated code.
 *
 */
public class MultiVals {

	public static boolean canReturnMultiVal(IrNode source) {
		var concrete = source.concreteNode();
		return concrete instanceof FunctionCallExpr
				|| concrete instanceof VariableExpr expr && expr.source() == LuaLocalVar.VARARGS;
	}
	
	// Called from generated code
	
	private static final Type THIS = Type.of(MultiVals.class);
	
	public static final CallTarget SPREAD_FIRST = THIS
			.staticMethod(Type.OBJECT, "spreadFirst", Type.OBJECT);
	
	public static Object spreadFirst(Object value) {
		return value instanceof Object[] array ? array[0] : value;
	}
	
	public static final CallTarget SPREAD_REST = THIS
			.staticMethod(Type.OBJECT, "spreadRest", Type.OBJECT, Type.INT);
	
	public static Object spreadRest(Object value, int index) {
		return value instanceof Object[] array && array.length > index ? array[index] : null;
	}
	
	public static final CallTarget ARRAY_LENGTH = THIS.staticMethod(Type.INT, "arrayLength", Type.OBJECT);
	
	public static int arrayLength(Object maybeArray) {
		return maybeArray instanceof Object[] array ? array.length : 1;
	}
	
	public static final CallTarget EXTEND_ARRAY = THIS
			.staticMethod(Type.VOID, "extendArray", Type.OBJECT.array(1), Type.INT, Type.OBJECT);
	
	public static void extendArray(Object[] results, int pos, Object last) {
		if (last instanceof Object[] array) {
			System.arraycopy(array, 0, results, pos, array.length);
		} else {
			results[results.length - 1] = last;
		}
	}
}
