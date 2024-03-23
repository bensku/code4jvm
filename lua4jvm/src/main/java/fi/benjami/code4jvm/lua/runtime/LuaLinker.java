package fi.benjami.code4jvm.lua.runtime;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;

import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.call.FixedCallTarget;
import fi.benjami.code4jvm.lua.compiler.FunctionCompiler;
import fi.benjami.code4jvm.lua.debug.LuaDebugOptions;
import fi.benjami.code4jvm.lua.ir.LuaType;

/**
 * Dynamic linker for function calls and table access from Lua.
 * 
 * <p>This relies heavily on <code>invokedynamic</code> and especially
 * {@link MethodHandles#guardWithTest(MethodHandle, MethodHandle, MethodHandle)}.
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
					MethodType.methodType(MethodHandle.class, LuaCallSite.class, LuaType[].class, Object.class, Object[].class));
		} catch (NoSuchMethodException | IllegalAccessException e) {
			throw new AssertionError(e);
		}
	}
	
	/**
	 * Links a call from Lua.
	 * @param meta Call site metadata.
	 * @param types Compile-time types at the call site.
	 * @param callable The callable object. This could be a function or e.g. a
	 * {@link TableAccess table accessor}.
	 * @param args Runtime arguments for the call.
	 * @return A call target that contains target and (optionally) guard
	 * method handles.
	 */
	public static LuaCallTarget linkCall(LuaCallSite meta, LuaType[] types, Object callable, Object... args) {
		// Debugging and integration testing hook
		if (LuaDebugOptions.linkerTrace != null) {
			LuaDebugOptions.linkerTrace.metadata = meta;
			LuaDebugOptions.linkerTrace.callable = callable;
		}
		
		MethodHandle target;
		MethodHandle guard;
		if (callable instanceof LuaFunction function) {
			if (meta.shouldUseRuntimeTypes() && meta.site != null) {
				// If call site has types that were unknown when it was compiled,
				// we'll try to compile a specialization based on runtime types
				// This is only done a few times, so we're not constantly relinking if
				// the types keep changing
				// Types are "checked" by casting and relinking if CCE was thrown, so
				// the runtime overhead should be small when they don't change
				// TODO can we get this working even if meta.site == null?
				target = RuntimeTypeAnalyzer.specialize(meta, function, args);
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
			guard = meta.shouldCheckTarget() ? TARGET_HAS_CHANGED.bindTo(meta.currentCallable) : PROTOTYPE_HAS_CHANGED.bindTo(meta.currentPrototype);
		} else if (callable instanceof MethodHandle handle) {
			// Calling from Lua to Java
			// TODO something better than this
			target = MethodHandles.dropArguments(handle, 0, Object.class);
			guard = TARGET_HAS_CHANGED.bindTo(meta.currentCallable);
		} else if (callable instanceof DynamicTarget dt) {
			return dt.resolve(meta, args);
		} else {
			// TODO metatables
			throw new UnsupportedOperationException(callable.getClass().getName() + " '" + callable + "' is not callable");			
		}
		
		return new LuaCallTarget(target, guard);
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
	private static MethodHandle updateSite(LuaCallSite meta, LuaType[] types,
			Object callable, Object... args) {
		var site = meta.site;
		meta.linkageCount++;
		
		// Compile the method
		var linkTarget = linkCall(meta, types, callable, args);
		var target = linkTarget.target().asType(site.type());
		
		// Set call site to target a new guarded handle, in case callable changes again
		meta.currentCallable = callable;
		if (linkTarget.guard() != null) {			
			site.setTarget(guardedHandle(meta, target, linkTarget.guard()));		
		} else {
			site.setTarget(target);
		}
		
		// But this time, proceed directly to target
		return target;
	}
	
	static MethodHandle guardedHandle(LuaCallSite meta, MethodHandle target, MethodHandle guard) {
		var fallback = MethodHandles.foldArguments(MethodHandles.spreadInvoker(target.type(), 1), meta.relink)
				.asVarargsCollector(Object[].class)
				.asType(target.type());
		return MethodHandles.guardWithTest(guard, target, fallback);
	}
	
	public static final FixedCallTarget BOOTSTRAP_DYNAMIC = TYPE.staticMethod(Type.of(CallSite.class), "dynamic",
			Type.of(MethodHandles.Lookup.class), Type.STRING, Type.of(MethodType.class), Type.of(LuaType[].class));

	public static CallSite dynamic(MethodHandles.Lookup lookup, String name, MethodType type,
			LuaType[] types) {
		var site = new MutableCallSite(type);
		var meta = new LuaCallSite(site, types);
		var handle = MethodHandles.foldArguments(MethodHandles.spreadInvoker(type, 1), meta.relink)
				.asVarargsCollector(Object[].class) // TODO try to use asCollector() instead
				.asType(type);
		site.setTarget(handle);
		return site;
	}
	
	public static final FixedCallTarget BOOTSTRAP_CONSTANT = TYPE.staticMethod(Type.of(CallSite.class), "constant",
			Type.of(MethodHandles.Lookup.class), Type.STRING, Type.of(MethodType.class), Type.of(LuaType[].class));
	
	public static CallSite constant(MethodHandles.Lookup lookup, String name, MethodType type,
			LuaType[] types) {
		// TODO currently unused, but constant call sites could be used for functions that do not access upvalues
		return dynamic(lookup, name, type, types);
	}
	
}
