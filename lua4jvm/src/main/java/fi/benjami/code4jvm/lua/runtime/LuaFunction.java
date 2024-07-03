package fi.benjami.code4jvm.lua.runtime;

import java.util.Arrays;
import java.util.Objects;

import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.lua.LuaVm;
import fi.benjami.code4jvm.lua.compiler.FunctionCompiler;
import fi.benjami.code4jvm.lua.ir.LuaType;

public record LuaFunction(
		/**
		 * The Lua VM that owns this function.
		 */
		LuaVm owner,
		
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
	
	public LuaFunction {
		assert owner != null;
	}
	
	public LuaFunction(LuaVm vm, LuaType.Function type, Object[] upvalues) {
		this(vm, type, upvalues, Arrays.stream(upvalues)
				.map(LuaType::of)
				.toArray(LuaType[]::new));
	}
	
	public Object call(Object... args) throws Throwable {
		var types = Arrays.stream(args)
				.map(LuaType::of)
				.toArray(LuaType[]::new);
		var handle = FunctionCompiler.callTarget(types, this, true, false);
		// DO NOT bind anything to the handle, this will break varargs!
		var acceptedArgCount = type.isVarargs() ? args.length : type.acceptedArgs().size();
		var realArgs = new Object[acceptedArgCount + 1];
		realArgs[0] = this;
		System.arraycopy(args, 0, realArgs, 1, acceptedArgCount);
		var result = handle.invokeWithArguments(realArgs);
		
		// Convert function calls that return multival of one to just the value
		// E.g. nested function calls may lead to multivals being returned, but usually don't
		// However, for Java callees we lack information the specialize call targets
		if (result instanceof Object[] array && array.length == 1) {
			return array[0]; // So just pick the first element
		}
		return result;
	}
	
}
