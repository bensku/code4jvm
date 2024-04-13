package fi.benjami.code4jvm.lua.ffi;

import java.lang.invoke.MethodHandle;
import java.util.List;

import fi.benjami.code4jvm.lua.ir.LuaType;

/**
 * A set of Java methods that are callable from Lua using FFI.
 *
 */
public record JavaFunction(
		/**
		 * Function name, as exposed to Lua.
		 */
		String name,
		
		/**
		 * Possible targets for this function. The runtime will try these in
		 * order and produce an error if none of them match the call site.
		 */
		List<Target> targets,
		
		/**
		 * Fallback target. This is matched after all other targets AND
		 * directly used when types at call site have changed too often.
		 */
		Target fallback
) {
	
	public record Target(
			/**
			 * Arguments that are injected by Lua VM at the call site.
			 * Injected arguments must come first!
			 */
			List<Class<?>> injectedArgs,
			
			/**
			 * Function arguments. All are required, use multiple targets
			 * (i.e. Java overloads) if some should be optional.
			 */
			List<Arg> arguments,
			
			/**
			 * Whether or not the last argument has variable arity.
			 * The method must have an array of appropriate type as last
			 * argument if this is true.
			 */
			boolean varargs,
			
			/**
			 * Type of return values.
			 */
			LuaType returnType,
			
			/**
			 * Whether or not there are multiple return values.
			 * If there are, they must all be of same Lua type.
			 */
			boolean multipleReturns,
			
			/**
			 * Java method that should be called for this target.
			 */
			MethodHandle method
	) {}
	
	public record Arg(String name, LuaType type) {}
	
	// TODO support functions for generating errors
	
	public Target matchToArgs(LuaType[] argTypes) {
		// Try all targets in order
		for (var target : targets) {
			if (checkArgs(target, argTypes) == MatchResult.SUCCESS) {
				return target;
			}
		}
		
		// No match? Use fallback, if it exists
		if (fallback != null) {
			return fallback;
		}
		
		return null;
	}
	
	private enum MatchResult {
		SUCCESS,
		TOO_FEW_ARGS,
		ARG_TYPE_MISMATCH,
		VARARGS_TYPE_MISMATCH
	}
	
	private MatchResult checkArgs(Target target, LuaType[] argTypes) {
		// Check that call site has enough arguments
		var requiredArgs = target.arguments.size();
		if (target.varargs) {
			requiredArgs--; // Zero arguments is totally valid
		}
		if (argTypes.length < requiredArgs) {
			return MatchResult.TOO_FEW_ARGS;
		}
		
		// Check types of arguments
		for (var i = 0; i < requiredArgs; i++) {
			if (!target.arguments.get(i).type.isAssignableFrom(argTypes[i])) {
				return MatchResult.ARG_TYPE_MISMATCH;
			}
		}
		
		// If this is varargs target, check that rest of the arguments have correct type
		if (target.varargs) {			
			var last = target.arguments.get(requiredArgs).type;
			for (var i = requiredArgs; i < argTypes.length; i++) {
				if (!last.isAssignableFrom(argTypes[i])) {
					return MatchResult.VARARGS_TYPE_MISMATCH;
				}
			}
		}
		
		return MatchResult.SUCCESS;
	}
}
