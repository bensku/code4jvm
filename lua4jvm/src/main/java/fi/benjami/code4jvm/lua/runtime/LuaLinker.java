package fi.benjami.code4jvm.lua.runtime;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;
import java.util.Arrays;
import java.util.stream.Stream;

import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.call.FixedCallTarget;
import fi.benjami.code4jvm.lua.compiler.FunctionCompiler;
import fi.benjami.code4jvm.lua.debug.LuaDebugOptions;
import fi.benjami.code4jvm.lua.ffi.JavaFunction;
import fi.benjami.code4jvm.lua.ir.LuaType;
import fi.benjami.code4jvm.lua.stdlib.LuaException;

/**
 * Dynamic linker for function calls and table access from Lua.
 * 
 * <p>This relies heavily on <code>invokedynamic</code> and especially
 * {@link MethodHandles#guardWithTest(MethodHandle, MethodHandle, MethodHandle)}.
 *
 */
public class LuaLinker {
	
	private static final int RUNTIME_TYPES_MAX_CHANGES = 3, LUA_FUNC_PROTOTYPE_MAX_CHANGES = 5;
	
	private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
	public static final Type TYPE = Type.of(LuaLinker.class);
	
	static final MethodHandle TARGET_HAS_CHANGED, PROTOTYPE_HAS_CHANGED, UPDATE_SITE,
		NEW_WRAPPER_EX, WRAPPER_EX_CAUSE;
	
	private static final class WrapperException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		@SuppressWarnings("unused") // MethodHandle
		public WrapperException(RuntimeException e) {
			super(e);
		}
		
		@Override
		public Throwable fillInStackTrace() {
			return this;
		}
	}
	
	static {
		try {
			TARGET_HAS_CHANGED = LOOKUP.findStatic(LuaLinker.class, "checkTargetHasChanged",
					MethodType.methodType(boolean.class, Object.class, Object.class));
			PROTOTYPE_HAS_CHANGED = LOOKUP.findStatic(LuaLinker.class, "checkPrototypeHasChanged",
					MethodType.methodType(boolean.class, Object.class, Object.class));
			UPDATE_SITE = LOOKUP.findStatic(LuaLinker.class, "updateSite",
					MethodType.methodType(MethodHandle.class, LuaCallSite.class, LuaType[].class, Object.class, Object[].class));
			NEW_WRAPPER_EX = LOOKUP.findConstructor(WrapperException.class, MethodType.methodType(void.class, RuntimeException.class));
			WRAPPER_EX_CAUSE = LOOKUP.findVirtual(WrapperException.class, "getCause", MethodType.methodType(Throwable.class));
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
			if (callable instanceof LuaFunction function) {				
				LuaDebugOptions.linkerTrace.currentPrototype = function.type();
			}
		}

		MethodHandle target;
		MethodHandle guard;
		if (callable instanceof LuaFunction function) {
			// Prefer guard on function instance, unless that changes too often
			// (because this allows us to consider captured upvalue types)
			var checkTarget = meta.linkageCount < LUA_FUNC_PROTOTYPE_MAX_CHANGES;
			// If the call site has unknown types, try to specialize based on
			// runtime types of the arguments - unless the types change too often
			var runtimeTypes = meta.hasUnknownTypes && meta.typeChangeCount < RUNTIME_TYPES_MAX_CHANGES;
			var specializedTypes = runtimeTypes ?
					Arrays.stream(args).map(LuaType::of).toArray(LuaType[]::new) : types;

			target = FunctionCompiler.callTarget(specializedTypes, function, checkTarget);
			guard = checkTarget ? TARGET_HAS_CHANGED.bindTo(function)
					: PROTOTYPE_HAS_CHANGED.bindTo(function.type());
			meta.usesRuntimeTypes = runtimeTypes;
		} else if (callable instanceof JavaFunction function) {
			// Java method exposed to Lua via FFI
			var specializedTypes = types;
			JavaFunction.Target funcTarget;
			if (meta.hasUnknownTypes) {
				// We have arguments with unknown types at compile time
				if (meta.typeChangeCount < RUNTIME_TYPES_MAX_CHANGES) {
					// Use them! Runtime types are never LESS applicable than compile-time types
					// so we don't need to check anything else
					specializedTypes = Arrays.stream(args).map(LuaType::of).toArray(LuaType[]::new);
					funcTarget = function.matchToArgs(specializedTypes);
					meta.usesRuntimeTypes = true;
				} else {
					// Too many type changes; we'd prefer to use compile-time types
					funcTarget = function.matchToArgs(types);
					if (funcTarget == null) {
						// But it is entirely possible that we can't!
						// Performance be damned, a call that has correct types at runtime must not fail
						specializedTypes = Arrays.stream(args).map(LuaType::of).toArray(LuaType[]::new);
						funcTarget = function.matchToArgs(specializedTypes);
						meta.usesRuntimeTypes = true;
					} else {
						meta.usesRuntimeTypes = false;
					}
				}
			} else {
				// All argument types are known compile-time
				funcTarget = function.matchToArgs(types);
				meta.usesRuntimeTypes = false;
			}
			
			if (funcTarget == null) {
				// Failed to match any of the targets available
				// TODO better error reporting, using the runtime types
				throw new LuaException(function.name() + ": invalid arguments");
			}

			// Drop unused trailing arguments and self argument; target is not expecting them
			target = dropUnusedArguments(funcTarget.method(), specializedTypes);
			target = MethodHandles.dropArguments(target, 0, Object.class);
			guard = TARGET_HAS_CHANGED.bindTo(function);
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
		
		// Link to the target method (which might need to be compiled)
		var linkTarget = linkCall(meta, types, callable, args);
		var target = linkTarget.target();
		
		if (meta.usesRuntimeTypes) {
			// Guard against type changes using exceptions and MethodHandle dark magic
			target = catchTypeChange(meta, target, types);
			meta.typeChangeCount++; // FIXME this might not be type change!
		} else {
			// Just cast; linkCall() would have thrown if this was not safe
			target = target.asType(site.type());
		}
		
		if (linkTarget.guard() != null) {
			site.setTarget(guardedHandle(meta, target, linkTarget.guard()));		
		} else {
			site.setTarget(target);
		}
		
		// But this time, proceed directly to target
		return target;
	}
	
	private static MethodHandle dropUnusedArguments(MethodHandle target, LuaType[] argTypes) {
		var type = target.type();
		if (type.parameterCount() == argTypes.length) {
			return target;
		}
		
		// Force linker to cast unused parameters to objects (this is easiest)
		var dropped = new Class[argTypes.length - type.parameterCount()];
		Arrays.fill(dropped, Object.class);
		return MethodHandles.dropArguments(target, type.parameterCount(), dropped);
	}
	
	static MethodHandle guardedHandle(LuaCallSite meta, MethodHandle target, MethodHandle guard) {
		var fallback = MethodHandles.foldArguments(MethodHandles.spreadInvoker(target.type(), 1), meta.relink)
				.asVarargsCollector(Object[].class)
				.asType(target.type());
		return MethodHandles.guardWithTest(guard, target, fallback);
	}
	
	private static MethodHandle catchTypeChange(LuaCallSite meta, MethodHandle target, LuaType[] types) {
		// We detect invalid type changes by catching CCE or NPE
		// However, the underlying method may also throw them
		// In this case, we'll use a temporary wrapper to swallow unrelated exceptions
		// TODO bypass this whenever possible, this is probably not great for performance
		/*
		 * try {
		 *     args = cast(args)
		 *     try {
		 *         result = target(args)
		 *     } catch (Exception e) {
		 *         throw new WrapperException(e)
		 *     }
		 * } catch (ClassCastException | NullPointerException e) {
		 *     relink();
		 * } catch (Exception e) {
		 *     throw e.getCause();
		 * }
		 */

		var wrapAndRethrow = MethodHandles.foldArguments(
				MethodHandles.dropArguments(MethodHandles.throwException(target.type().returnType(), WrapperException.class), 1, RuntimeException.class), 
				NEW_WRAPPER_EX
		);
		var unwrapAndThrow = MethodHandles.foldArguments(
				MethodHandles.dropArguments(MethodHandles.throwException(meta.site.type().returnType(), Throwable.class), 1, WrapperException.class),
				WRAPPER_EX_CAUSE
		);
		// IMPORTANT: use the site, NOT target type here!
		// Otherwise, the fallback path is not actually capable of accepting the type change...
		// This results in rather cryptic errors
		// (don't ask how I know)
		var fallback = MethodHandles.foldArguments(MethodHandles.spreadInvoker(meta.site.type(), 1), meta.relink)
				.asVarargsCollector(Object[].class)
				.asType(meta.site.type());
		
		// Catch all runtime exceptions (first common superclass of CCE and NPE) from target
		var callTarget = MethodHandles.catchException(target, RuntimeException.class, wrapAndRethrow);
		// Try to cast target to expected type
		var castArgs = callTarget.asType(meta.site.type());
		// Relink on CCE or NPE from type cast (target-thrown exceptions are wrapped)
		var guardArgs = MethodHandles.catchException(
				MethodHandles.catchException(castArgs, NullPointerException.class, MethodHandles.dropArguments(fallback, 0, NullPointerException.class)),
				ClassCastException.class, MethodHandles.dropArguments(fallback, 0, ClassCastException.class));
		// Re-throw exception caught from target, if any
		var unwrap = MethodHandles.catchException(guardArgs, WrapperException.class, unwrapAndThrow);
		return unwrap;
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
