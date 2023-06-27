package fi.benjami.code4jvm.lua.runtime;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import fi.benjami.code4jvm.lua.compiler.FunctionCompiler;
import fi.benjami.code4jvm.lua.ir.LuaType;

/**
 * Analyzes runtime types of function arguments and compiles specializations
 * of Lua functions based on them.
 *
 */
class RuntimeTypeAnalyzer {
	
	private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
	
	private static final MethodHandle SPECIALIZE;
	
	static {
		try {
			SPECIALIZE = LOOKUP.findStatic(RuntimeTypeAnalyzer.class, "specialize",
					MethodType.methodType(MethodHandle.class, LuaCallSite.class, LuaFunction.class, Object[].class));
		} catch (NoSuchMethodException | IllegalAccessException e) {
			throw new AssertionError(e);
		}
	}

	/**
	 * Gets handle to a method that
	 * <ol>
	 * <li>Inspects <i>runtime</i> types of arguments at the call site
	 * <li>Compiles a specialization based on them
	 * <li>Sets the call site to use the new specialization (with guards)
	 * <li>Directly calls the specialization
	 * </ol>
	 * @param meta Call site metadata.
	 * @param function The Lua function.
	 * @return
	 */
	public static MethodHandle bridgeCompiler(LuaCallSite meta, LuaFunction function) {
		// Capture metadata and target function as arguments to specialization creator
		// Accept arguments as array to check their types
		var specialize = MethodHandles.insertArguments(SPECIALIZE, 0, meta, function);
		// Collect arguments as array for specialization creator
		// Unwrap them from array and invoke the specialized method
		return MethodHandles.foldArguments(MethodHandles.spreadInvoker(meta.site.type(), 0), specialize)
				.asVarargsCollector(Object[].class);
	}
	
	@SuppressWarnings("unused") // MethodHandle
	private static MethodHandle specialize(LuaCallSite meta, LuaFunction function, Object... args) {
		// Compute types of arguments, using runtime information
		// (first argument is the callable, which we know is Lua function)
		var argTypes = new LuaType[args.length - 1];
		for (var i = 1; i < args.length; i++) {
			argTypes[i - 1] = LuaType.of(args[i]);
		}
		
		// Compile the function specialization
		var target = FunctionCompiler.callTarget(argTypes, function, meta.shouldCheckTarget());
		
		// Cast provided arguments to type of our specialization
		// If this fails runtime (i.e. throws CCE), relink the call as usual
		var castToTarget = MethodHandles.catchException(
				MethodHandles.explicitCastArguments(target, meta.site.type()),
				ClassCastException.class,
				MethodHandles.dropArguments(MethodHandles.foldArguments(MethodHandles.exactInvoker(meta.site.type()), meta.relink),
						0, ClassCastException.class)
				);
		// TODO detect when Java code called by Lua code throws CCE?
		// Ideally, do it in exception handler since that is the cold path
		
		// If the function object changes, relink the site as usual
		var guard = LuaLinker.guardedHandle(meta, castToTarget);
		
		// Skip call to this method (specialize(...)) in subsequent calls
		meta.site.setTarget(guard);
		
		// Proceed directly into specialization (skipping guard)
		return castToTarget;
	}
}
