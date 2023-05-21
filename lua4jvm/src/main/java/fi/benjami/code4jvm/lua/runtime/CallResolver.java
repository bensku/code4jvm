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
 * Static methods for resolving {@code invokedynamic} function calls from Lua
 * code.
 *
 */
public class CallResolver {
	
	private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
	public static final Type TYPE = Type.of(CallResolver.class);
	
	private static class CallableHolder {
		Object callable;
	}
	
	private static final MethodHandle HAS_CHANGED, UPDATE_SITE;
	
	static {
		try {
			HAS_CHANGED = LOOKUP.findStatic(CallResolver.class, "hasChanged",
					MethodType.methodType(boolean.class, CallableHolder.class, Object.class));
			UPDATE_SITE = LOOKUP.findStatic(CallResolver.class, "updateSite",
					MethodType.methodType(MethodHandle.class, CallableHolder.class, MutableCallSite.class, LuaType[].class, Object.class));
		} catch (NoSuchMethodException | IllegalAccessException e) {
			throw new AssertionError(e);
		}
	}
	
	@SuppressWarnings("unused") // MethodHandle
	private static boolean hasChanged(CallableHolder expected, Object actual) {
		// TODO what about inline functions?
		return expected.callable == actual;
	}
	
	@SuppressWarnings("unused") // MethodHandle
	private static MethodHandle updateSite(CallableHolder holder, MutableCallSite site, LuaType[] types, Object callable) {
		// Compile the method
		var target = FunctionCompiler.callTarget(types, callable).asType(site.type());
		
		// Set call site to target a new guarded handle, in case callable changes again
		holder.callable = callable;
		site.setTarget(guardedHandle(holder, site, types, target));
		
		// But this time, proceed directly to target
		return target;
	}
	
	private static MethodHandle guardedHandle(CallableHolder holder, MutableCallSite site, LuaType[] types, MethodHandle target) {
		// Call the code we're compiling now if the callable is unchanged
		var test = HAS_CHANGED.bindTo(holder);
		
		// If callable HAS changed, update the call site to a new guarded handle, then proceed to target
		var updateSite = MethodHandles.insertArguments(UPDATE_SITE, 0, holder, site, types);
		var fallback = MethodHandles.foldArguments(MethodHandles.exactInvoker(target.type()), updateSite);
		return MethodHandles.guardWithTest(test, target, fallback);
	}
	
	public static final FixedCallTarget BOOTSTRAP_DYNAMIC = TYPE.staticMethod(Type.of(CallSite.class), "dynamic",
			Type.of(MethodHandles.Lookup.class), Type.STRING, Type.of(MethodType.class), Type.of(LuaType[].class));

	public static CallSite dynamic(MethodHandles.Lookup lookup, String name, MethodType type,
			LuaType[] types) {
		var site = new MutableCallSite(type);
		var holder = new CallableHolder();
		var updateSite = MethodHandles.insertArguments(UPDATE_SITE, 0, holder, site, types);
		var handle = MethodHandles.foldArguments(MethodHandles.exactInvoker(type), updateSite);
		site.setTarget(handle);
		return site;
	}
	
	public static final FixedCallTarget BOOTSTRAP_CONSTANT = TYPE.staticMethod(Type.of(CallSite.class), "constant",
			Type.of(MethodHandles.Lookup.class), Type.STRING, Type.of(MethodType.class), Type.of(LuaType[].class));
	
	public static CallSite constant(MethodHandles.Lookup lookup, String name, MethodType type,
			LuaType[] types) {
		return dynamic(lookup, name, type, types);
	}
	
}
