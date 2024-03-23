package fi.benjami.code4jvm.lua.runtime;

import java.lang.invoke.MethodHandle;

/**
 * A call target for dynamic linkage.
 * 
 * @see LuaLinker
 *
 */
public record LuaCallTarget(
		/**
		 * Method to call.
		 */
		MethodHandle target,
		
		/**
		 * Guard that should be checked before calling the method. If this
		 * fails (returns false), the call site will be relinked.
		 * Nullable; if absent, the call site cannot be relinked.
		 */
		MethodHandle guard
) {}
