package fi.benjami.code4jvm.lua.runtime;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;
import java.util.Arrays;

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
	
	private static final int RUNTIME_TYPES_MAX_CHANGES = 3, LUA_FUNC_INSTANCE_MAX_CHANGES = 5;
	
	private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
	public static final Type TYPE = Type.of(LuaLinker.class);
	
	static final MethodHandle TARGET_HAS_CHANGED, PROTOTYPE_HAS_CHANGED, TYPE_HAS_CHANGED,
			ARRAY_FIRST, SHAPE_ARRAYS, UPDATE_SITE, NEW_WRAPPER_EX, WRAPPER_EX_CAUSE;
	
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
			TYPE_HAS_CHANGED = LOOKUP.findStatic(LuaLinker.class, "checkTypeHasChanged",
					MethodType.methodType(boolean.class, Class.class, Object.class));
			ARRAY_FIRST = LOOKUP.findStatic(LuaLinker.class, "getArrayFirst",
					MethodType.methodType(Object.class, Object[].class));
			SHAPE_ARRAYS = LOOKUP.findStatic(LuaLinker.class, "shapeArrays",
					MethodType.methodType(Object[].class, int.class, Object[].class));
			UPDATE_SITE = LOOKUP.findStatic(LuaLinker.class, "updateSite",
					MethodType.methodType(MethodHandle.class, LuaCallSite.class, Object.class, Object[].class));
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
	public static LuaCallTarget linkCall(LuaCallSite meta, Object callable, Object... args) {
		// Debugging and integration testing hook
		if (LuaDebugOptions.linkerTrace != null) {
			LuaDebugOptions.linkerTrace.metadata = meta;
			LuaDebugOptions.linkerTrace.callable = callable;
			if (callable instanceof LuaFunction function) {				
				LuaDebugOptions.linkerTrace.currentPrototype = function.type();
			}
		}

		var compiledTypes = meta.options.types();
		MethodHandle target;
		MethodHandle guard;
		if (callable instanceof LuaFunction function) {
			// Prefer guard on function instance, unless that changes too often
			// (because this allows us to consider captured upvalue types)
			var checkTarget = meta.linkageCount < LUA_FUNC_INSTANCE_MAX_CHANGES;
			// If the call site has unknown types, try to specialize based on
			// runtime types of the arguments - unless the types change too often
			var runtimeTypes = meta.hasUnknownTypes && meta.linkageCount < RUNTIME_TYPES_MAX_CHANGES
					&& !meta.options.spreadArguments();
			// FIXME runtime type guards currently wreak havoc on varargs collection
			var specializedTypes = runtimeTypes ?
					Arrays.stream(args).map(LuaType::of).toArray(LuaType[]::new) : compiledTypes;
			if (meta.options.spreadArguments() && specializedTypes.length < function.type().acceptedArgs().size()) {
				// If we have fewer arguments than what the function can take
				// fill rest with unknown types, as the last might be spread over them
				specializedTypes = Arrays.copyOf(specializedTypes, function.type().acceptedArgs().size());
				Arrays.fill(specializedTypes, compiledTypes.length, specializedTypes.length, LuaType.UNKNOWN);
			}

			// Truncate multival return if site doesn't want to spread
			target = FunctionCompiler.callTarget(specializedTypes, function, checkTarget,
					!meta.options.spreadResults());
			guard = checkTarget ? TARGET_HAS_CHANGED.bindTo(function)
					: PROTOTYPE_HAS_CHANGED.bindTo(function.type());
			meta.usesRuntimeTypes = runtimeTypes;
			
			if (!function.type().isVarargs()) {
				// Drop unnecessary arguments that target won't accept
				target = dropUnusedArguments(target, specializedTypes, 1);
			}
		} else if (callable instanceof JavaFunction function) {
			// Java method exposed to Lua via FFI
			var specializedTypes = compiledTypes;
			JavaFunction.Target funcTarget;
			if (meta.hasUnknownTypes) {
				// We have arguments with unknown types at compile time
				if (meta.linkageCount < RUNTIME_TYPES_MAX_CHANGES) {
					// Use them! Runtime types are never LESS applicable than compile-time types
					// so we don't need to check anything else
					specializedTypes = Arrays.stream(args).map(LuaType::of).toArray(LuaType[]::new);
					funcTarget = function.matchToArgs(specializedTypes);
					meta.usesRuntimeTypes = true;
				} else {
					// Too many type changes; we'd prefer to use compile-time types
					funcTarget = function.matchToArgs(compiledTypes);
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
				funcTarget = function.matchToArgs(compiledTypes);
				meta.usesRuntimeTypes = false;
			}
			
			if (funcTarget == null) {
				// Failed to match any of the targets available
				// TODO better error reporting, using the runtime types
				throw new LuaException(function.name() + ": invalid arguments");
			}

			target = funcTarget.method();
			if (!funcTarget.varargs()) {
				// Drop unused trailing arguments and self argument; target is not expecting them
				target = dropUnusedArguments(target, specializedTypes, 0);
			}
			
			// Call site wants to truncate a multival
			// We can't dynamically recompile the target, so do this on caller side
			if (funcTarget.multipleReturns() && !meta.options.spreadResults()) {
				target = MethodHandles.filterReturnValue(target, ARRAY_FIRST);
			}
			
			target = MethodHandles.dropArguments(target, 0, Object.class);
			if (funcTarget.varargs()) {
				// Tell JVM about varargs (this must be done after dropping arguments!)
				target = target.asVarargsCollector(Object[].class);
			}
			guard = TARGET_HAS_CHANGED.bindTo(function);
		} else if (callable instanceof DynamicTarget dt) {
			// TODO multival support; luckily not needed for table support
			return dt.resolve(meta, args);
		} else {
			// TODO metatables
			throw new UnsupportedOperationException(callable.getClass().getName() + " '" + callable + "' is not callable");			
		}
		
		if (meta.options.spreadArguments()) {
			// Last argument of this function call could be a multival
			if (args[args.length - 1] instanceof Object[] || compiledTypes.length == 1) {
				int requiredArgs = target.type().parameterCount() - 1; // self arg is never in varargs
				// Yeah, we have a multival that has a multival as its last element
				// We need a wrapper that merges them into one flat array
				var varargsIndex = args.length - 1;
				if (!target.isVarargsCollector()) {
					// One problem. The target does not accept Object[]!
					// But we can make it do that...
					target = MethodHandles.spreadInvoker(target.type(), 1)
							.bindTo(target);
					varargsIndex = 1;
				}
				var shapeFilter = MethodHandles.insertArguments(SHAPE_ARRAYS, 0, requiredArgs);
				target = MethodHandles.filterArguments(target, varargsIndex, shapeFilter)
						.asVarargsCollector(Object[].class);
			}
			// Use a guard for whatever the decision was
			var spreadGuard = MethodHandles.dropArguments(
					TYPE_HAS_CHANGED.bindTo(args[args.length - 1].getClass())
						.asType(MethodType.methodType(boolean.class, target.type().lastParameterType())),
					0,
					Arrays.copyOfRange(meta.site.type().parameterArray(), 0, meta.site.type().parameterCount() - 1)
			);
			return new LuaCallTarget(target, guard, spreadGuard);
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
	private static boolean checkTypeHasChanged(Class<?> expected, Object val) {
		return expected == val.getClass();
	}
	
	@SuppressWarnings("unused") // MethodHandle
	private static Object getArrayFirst(Object[] value) {
		return value.length != 0 ? value[0] : null;
	}
	
	@SuppressWarnings("unused") // MethodHandle
	private static Object[] shapeArrays(int expectedLen, Object[] outer) {
		if (outer[outer.length - 1] instanceof Object[] inner) {
			// Merge inner into outer
			var merged = new Object[Math.max(expectedLen, outer.length - 1 + inner.length)];
			System.arraycopy(outer, 0, merged, 0, outer.length - 1);
			System.arraycopy(inner, 0, merged, outer.length - 1, inner.length);
			return merged;
		} else {
			return outer.length >= expectedLen ? outer : Arrays.copyOf(outer, expectedLen);
		}
	}
	
	@SuppressWarnings("unused") // MethodHandle
	private static MethodHandle updateSite(LuaCallSite meta, Object callable, Object... args) {
		var site = meta.site;
		meta.linkageCount++;
		
		// Link to the target method (which might need to be compiled)
		var linkTarget = linkCall(meta, callable, args);
		var target = linkTarget.target();
		
		if (meta.usesRuntimeTypes) {
			// Guard against type changes using exceptions and MethodHandle dark magic
			target = catchTypeChange(meta, target);
		} else {
			// Just cast; linkCall() would have thrown if this was not safe
			target = target.asType(site.type());
		}
		
		// If there are guards, compose them on top of each other
		var guardedTarget = target;
		var guards = linkTarget.guards();
		for (var guard : guards) {
			guardedTarget = guardedHandle(meta, guardedTarget, guard);
		}
		
		// Set site to use the target with guards...
		site.setTarget(guardedTarget);
		return target; // But skip them this time, we've already done necessary checks
	}
	
	private static MethodHandle dropUnusedArguments(MethodHandle target, LuaType[] argTypes, int leadingCount) {
		var type = target.type();
		if (type.parameterCount() >= argTypes.length) {
			return target;
		}
		
		var dropped = Arrays.stream(argTypes)
			.skip(type.parameterCount() - leadingCount)
			.map(LuaType::backingType)
			.map(Type::loadedClass)
			.toArray(Class[]::new);
		
		return MethodHandles.dropArguments(target, type.parameterCount(), dropped);
	}
	
	static MethodHandle guardedHandle(LuaCallSite meta, MethodHandle target, MethodHandle guard) {
		var fallback = MethodHandles.foldArguments(MethodHandles.spreadInvoker(target.type(), 1), meta.relink)
				.asVarargsCollector(Object[].class)
				.asType(target.type());
		return MethodHandles.guardWithTest(guard, target, fallback);
	}
	
	private static MethodHandle catchTypeChange(LuaCallSite meta, MethodHandle target) {
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
			Type.of(MethodHandles.Lookup.class), Type.STRING, Type.of(MethodType.class), Type.of(CallSiteOptions.class));

	public static CallSite dynamic(MethodHandles.Lookup lookup, String name, MethodType type,
			CallSiteOptions options) {
		var site = new MutableCallSite(type);
		var meta = new LuaCallSite(site, options);
		// Use exact invoker if we're certain we're passing an array to varargs target
		var invoker = type.parameterCount() == 2 && type.lastParameterType().isArray() ?
				MethodHandles.exactInvoker(type) : MethodHandles.spreadInvoker(type, 1);
		var handle = MethodHandles.foldArguments(invoker, meta.relink)
				.asVarargsCollector(Object[].class) // TODO try to use asCollector() instead
				.asType(type);
		site.setTarget(handle);
		return site;
	}
	
	public static final FixedCallTarget BOOTSTRAP_CONSTANT = TYPE.staticMethod(Type.of(CallSite.class), "constant",
			Type.of(MethodHandles.Lookup.class), Type.STRING, Type.of(MethodType.class), Type.of(CallSiteOptions.class));
	
	public static CallSite constant(MethodHandles.Lookup lookup, String name, MethodType type,
			CallSiteOptions options) {
		// TODO currently unused, but constant call sites could be used for functions that do not access upvalues
		return dynamic(lookup, name, type, options);
	}
	
}
