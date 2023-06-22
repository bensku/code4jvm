package fi.benjami.code4jvm.lua.runtime;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;

import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.call.FixedCallTarget;
import fi.benjami.code4jvm.lua.compiler.FunctionCompiler;
import fi.benjami.code4jvm.lua.ir.LuaType;

/**
 * Static methods for linking {@code invokedynamic} function calls from Lua.
 *
 */
public class LuaLinker {
	
	private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
	public static final Type TYPE = Type.of(LuaLinker.class);
	
	static final MethodHandle TARGET_HAS_CHANGED, PROTOTYPE_HAS_CHANGED, UPDATE_SITE;
	
	static {
		try {
			TARGET_HAS_CHANGED = LOOKUP.findStatic(LuaLinker.class, "checkTargetHasChanged",
					MethodType.methodType(boolean.class, Object.class, Object.class));
			PROTOTYPE_HAS_CHANGED = LOOKUP.findStatic(LuaLinker.class, "checkPrototypeHasChanged",
					MethodType.methodType(boolean.class, Object.class, Object.class));
			UPDATE_SITE = LOOKUP.findStatic(LuaLinker.class, "updateSite",
					MethodType.methodType(MethodHandle.class, LuaCallSite.class, LuaType[].class, Object.class));
		} catch (NoSuchMethodException | IllegalAccessException e) {
			throw new AssertionError(e);
		}
	}
	
	/**
	 * Checks if the call target object has changed. This allows for
	 * specializations to use upvalue type information, but may cause excessive
	 * re-linking with e.g. inline functions passed as callbacks.
	 * @param previous
	 * @param next
	 * @return
	 */
	@SuppressWarnings("unused") // MethodHandle
	private static boolean checkTargetHasChanged(Object previous, Object next) {
		return previous == next;
	}
	
	/**
	 * Checks if the prototype of a Lua function call target has changed. This
	 * avoids re-linking calls to e.g. inline functions that are created within
	 * loops, but makes target upvalue type information unavailable.
	 * @param previous
	 * @param next
	 * @return
	 */
	@SuppressWarnings("unused") // MethodHandle
	private static boolean checkPrototypeHasChanged(Object previous, Object next) {
		return previous == ((LuaFunction) next).type();
	}
	
	@SuppressWarnings("unused") // MethodHandle
	private static MethodHandle updateSite(LuaCallSite meta, LuaType[] types, Object callable) {
		var site = meta.site;
		meta.linkageCount++;
		
		// Compile the method
		MethodHandle target;
		if (callable instanceof LuaFunction function) {
			if (meta.shouldUseRuntimeTypes()) {
				// If call site has types that were unknown when it was compiled,
				// we'll try to compile a specialization based on runtime types
				// This is only done a few times, so we're not constantly relinking if
				// the types keep changing
				// Types are "checked" by casting and relinking if CCE was thrown, so
				// the runtime overhead should be small when they don't change
				// TODO check if any of the types are unknown
				// TODO enter this path WITHOUT going through updateSite to speed up first-time calls?
				target = RuntimeTypeAnalyzer.bridgeCompiler(meta, function);
				meta.typeChangeCount++;
			} else {
				// If all types were known compile-time or the runtime types have changed multiple times
				// Just compile a specialization based on compile-time types
				// By default, we compare the current Lua function to previous one
				// and switch to comparing prototypes if it changes multiple times
				// When function prototypes are used, the compiler MUST NOT use upvalue types
				// because they could lead to miscompilations
				// TODO relax this if the prototype uses no upvalues? (note: _ENV is upvalue!)
				target = FunctionCompiler.callTarget(types, function, meta.shouldCheckTarget());
			}
			meta.currentPrototype = function.type();
		} else if (callable instanceof MethodHandle handle) {
			// Calling from Lua to Java
			// TODO something better than this
			target = MethodHandles.dropArguments(handle, 0, Object.class);
		} else {
			// TODO metatables
			throw new UnsupportedOperationException(callable + " is not callable");			
		}
		target = target.asType(site.type());
		
		// Set call site to target a new guarded handle, in case callable changes again
		meta.currentTarget = target;
		site.setTarget(guardedHandle(meta, target));		
		
		// But this time, proceed directly to target
		return target;
	}
	
	static MethodHandle guardedHandle(LuaCallSite meta, MethodHandle target) {
		MethodHandle test;
		if (meta.shouldCheckTarget()) {
			test = TARGET_HAS_CHANGED.bindTo(meta.currentTarget);
		} else {
			test = PROTOTYPE_HAS_CHANGED.bindTo(meta.currentPrototype);
		}
		
		var fallback = MethodHandles.foldArguments(MethodHandles.exactInvoker(target.type()), meta.relink);
		return MethodHandles.guardWithTest(test, target, fallback);
	}
	
	public static final FixedCallTarget BOOTSTRAP_DYNAMIC = TYPE.staticMethod(Type.of(CallSite.class), "dynamic",
			Type.of(MethodHandles.Lookup.class), Type.STRING, Type.of(MethodType.class), Type.of(LuaType[].class));

	public static CallSite dynamic(MethodHandles.Lookup lookup, String name, MethodType type,
			LuaType[] types) {
		var site = new MutableCallSite(type);
		var meta = new LuaCallSite(site, types);
		var handle = MethodHandles.foldArguments(MethodHandles.exactInvoker(type), meta.relink);
		site.setTarget(handle);
		return site;
	}
	
	public static final FixedCallTarget BOOTSTRAP_CONSTANT = TYPE.staticMethod(Type.of(CallSite.class), "constant",
			Type.of(MethodHandles.Lookup.class), Type.STRING, Type.of(MethodType.class), Type.of(LuaType[].class));
	
	public static CallSite constant(MethodHandles.Lookup lookup, String name, MethodType type,
			LuaType[] types) {
		// TODO currently unused, but constant call sites could be used for functions that only have the _ENV upvalue
		return dynamic(lookup, name, type, types);
	}
	
}
