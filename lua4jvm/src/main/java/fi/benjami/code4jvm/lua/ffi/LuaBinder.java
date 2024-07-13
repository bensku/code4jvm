package fi.benjami.code4jvm.lua.ffi;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import fi.benjami.code4jvm.lua.LuaVm;
import fi.benjami.code4jvm.lua.ir.LuaType;

/**
 * Tools for constructing {@link JavaFunction Java functions} that allow
 * Lua code to call (explicitly allowed) Java methods.
 * 
 * <p>It is also possible to construct JavaFunctions by hand. LuaBinder is
 * simply a more convenient and less error-prone way for doing that.
 *
 */
public class LuaBinder {
	
	private final MethodHandles.Lookup lookup;
	
	public LuaBinder(MethodHandles.Lookup lookup) {
		this.lookup = lookup;
	}
	
	/**
	 * Binds all {@link LuaExport exported} functions from a class.
	 * @param container Java class that contains static methods with
	 * {@link LuaExport} annotation.
	 * @return A collection of functions that can be called from Lua.
	 */
	public Collection<JavaFunction> bindFunctionsFrom(Class<?> container) {
		// Group methods by name
		var methods = new HashMap<String, List<Method>>();
		for (var method : container.getDeclaredMethods()) {
			var export = method.getAnnotation(LuaExport.class);
			if (export != null) {
				methods.computeIfAbsent(export.value(), k -> new ArrayList<>()).add(method);
			}
		}
		
		return methods.entrySet().stream()
				.map(entry -> bindFunction(entry.getKey(), toSorted(entry.getValue())))
				.toList();
	}
	
	private List<Method> toSorted(List<Method> methods) {
		// getDeclaredMethods() has no well-defined order...
		methods.sort(Comparator.comparing(Method::getParameterCount).reversed()); // Most parameters first
		// Intrinsics first
		Comparator<Method> notIntrinsic = Comparator.comparing(method -> method.isAnnotationPresent(LuaIntrinsic.class));
		methods.sort(notIntrinsic.reversed());
		// TODO least specific types last? fallback last?
		return methods;
	}
	
	public JavaFunction bindFunction(String name, List<Method> targetMethods) {
		var targets = new ArrayList<JavaFunction.Target>(targetMethods.size());
		JavaFunction.Target fallback = null;
		for (Method method : targetMethods) {
			var target = toFunctionTarget(method);
			if (method.isAnnotationPresent(Fallback.class)) {
				if (fallback != null) {
					throw new IllegalArgumentException("function " + name + " has more than one @Fallback");
				}
				fallback = target;
			} else {
				targets.add(target);
			}
		}
		// JVM doesn't care about method definition order
		// Make sure we always pick the target with most matching arguments
		Comparator<JavaFunction.Target> cmp = Comparator.comparingInt(target -> target.arguments().size());
		targets.sort(cmp.reversed());
		
		return new JavaFunction(name, targets, fallback);
	}

	private JavaFunction.Target toFunctionTarget(Method method) {
		var injectedArgs = new ArrayList<InjectedArg>();
		var args = new ArrayList<JavaFunction.Arg>();
		for (var param : method.getParameters()) {
			var inject = param.getAnnotation(Inject.class);
			if (inject != null) {
				if (!args.isEmpty()) {
					throw new IllegalArgumentException("injected arguments after normal ones in method " + method.getName());
				}
				injectedArgs.add(toInjectedArg(param.getType(), inject.value()));
			} else {
				args.add(toArg(param));
			}
		}

		// Figure out return type(s); array implies multiple returns
		var multipleReturns = method.getReturnType().isArray();
		var returnType = multipleReturns ? method.getReturnType().componentType() : method.getReturnType();
		
		// Get the actual method handle
		MethodHandle handle;
		try {
			handle = lookup.unreflect(method);
		} catch (IllegalAccessException e) {
			throw new IllegalStateException("lookup provided for LuaBinder has insufficient access", e);
		}
		
		var intrinsic = method.getAnnotation(LuaIntrinsic.class);
		var intrinsicId = intrinsic != null ? intrinsic.value() : null;
		return new JavaFunction.Target(injectedArgs, args, method.isVarArgs(), LuaType.of(returnType), multipleReturns, handle, intrinsicId);
	}
	
	private InjectedArg toInjectedArg(Class<?> type, String source) {
		// TODO support non-default sources
		if (!source.equals("default")) {
			throw new UnsupportedOperationException();
		}
		// Handle default injected arg source
		if (type.equals(LuaVm.class)) {
			return site -> site.options.owner();
		} else {
			throw new IllegalArgumentException("unsupported type for @Inject");
		}
	}
	
	private JavaFunction.Arg toArg(Parameter param) {
		return new JavaFunction.Arg(param.getName(), LuaType.of(param.getType()), param.isAnnotationPresent(Nullable.class));
	}
}
