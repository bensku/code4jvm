package fi.benjami.code4jvm.lua.runtime;

import java.lang.invoke.MethodHandle;
import java.util.function.BiFunction;

/**
 * Dynamic targets are callable like Lua functions. However, when first
 * called, they can decide where the method calls lead to. They can also
 * control when to trigger re-linkage.
 *
 */
public record DynamicTarget(
		/**
		 * Resolves which method this target should lead to.
		 * The function receives arguments from call site packaged (and boxed)
		 * into an object array.
		 */
		BiFunction<LuaCallSite, Object[], MethodHandle> resolve,
		
		/**
		 * Called each time this dynamic target is about to be entered.
		 * Receives the call target (which may or may not be this anymore!)
		 * and some/all of arguments passed to it.
		 * If this returns false, the call site is relinked.
		 */
		MethodHandle checkChanges
) {}
