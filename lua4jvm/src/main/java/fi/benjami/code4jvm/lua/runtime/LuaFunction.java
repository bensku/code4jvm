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
		Object[] upvalues,
		
		/**
		 * Types of upvalues.
		 */
		LuaType[] upvalueTypes
) {

	public static final Type TYPE = Type.of(LuaFunction.class);
	
	public LuaFunction(LuaType.Function type, Object[] upvalues) {
		this(type, upvalues, Arrays.stream(upvalues)
				.map(LuaType::of)
				.toArray(LuaType[]::new));
	}
	
	public Object call(Object... args) throws Throwable {
		var types = Arrays.stream(args)
				.map(LuaType::of)
				.toArray(LuaType[]::new);
		var handle = FunctionCompiler.callTarget(types, this, true);
		return handle.bindTo(this).invokeWithArguments(args);
	}
	
}
