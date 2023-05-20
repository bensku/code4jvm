package fi.benjami.code4jvm.lua.runtime;

import java.util.Arrays;

import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.lua.compiler.FunctionCompiler;
import fi.benjami.code4jvm.lua.ir.LuaType;

public record LuaFunction(
		/**
		 * Type (shape) of this function.
		 */
		LuaType.Function type,
		
		/**
		 * The values captured by this function when it was defined.
		 */
		Object[] upvalues
) {

	public static final Type TYPE = Type.of(LuaFunction.class);
	
	public Object call(Object... args) throws Throwable {
		var types = Arrays.stream(args)
				.map(arg -> arg != null ? LuaType.of(arg.getClass()) : LuaType.NIL)
				.toArray(LuaType[]::new);
		var handle = FunctionCompiler.callTarget(types, this);
		return handle.bindTo(this).invokeWithArguments(args);
	}
	
}
