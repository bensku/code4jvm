package fi.benjami.code4jvm.lua.compiler;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import fi.benjami.code4jvm.Constant;
import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.Variable;
import fi.benjami.code4jvm.flag.Access;
import fi.benjami.code4jvm.flag.FieldFlag;
import fi.benjami.code4jvm.flag.MethodFlag;
import fi.benjami.code4jvm.lua.ir.LuaLocalVar;
import fi.benjami.code4jvm.lua.ir.LuaType;
import fi.benjami.code4jvm.lua.ir.UpvalueTemplate;
import fi.benjami.code4jvm.lua.linker.LuaLinker;
import fi.benjami.code4jvm.lua.runtime.LuaFunction;
import fi.benjami.code4jvm.statement.Return;
import fi.benjami.code4jvm.typedef.ClassDef;

/**
 * Compiles specializations for {@link LuaFunction functions} and (in future)
 * other callables based on types of their upvalues and arguments.
 * 
 * @see LuaLinker
 *
 */
public class FunctionCompiler {
	
	public record CacheKey(
			List<LuaType> argTypes,
			List<LuaType> upvalueTypes,
			boolean truncateReturn
	) {}
	
	private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
	
	/**
	 * Fetches or compiles a specialization for given Lua function.
	 * @param argTypes Argument types to use for compilation.
	 * @param callable The Lua function object.
	 * @param useUpvalueTypes Whether or not the upvalue types should be
	 * considered known.
	 * @param truncateReturn If a multival would be returned, make the
	 * function return first value of it instead. This avoids unnecessary
	 * creation of Java arrays.
	 * @return Method handle that may be called.
	 */
	public static MethodHandle callTarget(LuaType[] argTypes, LuaFunction function, boolean useUpvalueTypes,
			boolean truncateReturn) {
		var upvalueTypes = useUpvalueTypes ? function.upvalueTypes() : function.type().upvalues().stream()
				.map(UpvalueTemplate::type)
				.toArray(LuaType[]::new);
		// TODO only use the argtypes in cache key that actually matter to this function
		var cacheKey = new CacheKey(List.of(argTypes), useUpvalueTypes ? List.of(upvalueTypes) : null, truncateReturn);
		
		// Compile and load the function code, or use something that is already cached
		var compiledFunc = function.type().specializations().computeIfAbsent(cacheKey, t -> {
			var ctx = LuaContext.forFunction(function.owner(), function.type(), truncateReturn, argTypes);
			var code = generateCode(ctx, function.type(), argTypes, upvalueTypes);
			try {
				// Load the class with single-use class loader
				// Using hidden classes would be preferable, but JVM hides them from stack frames
				// ... which really screws up stack traces of Lua code
				// See https://bugs.openjdk.org/browse/JDK-8212620
				var implClass = SingleClassLoader.load(toClassName(function.type().moduleName()), code);
				try {
					LOOKUP.findStaticSetter(implClass, ClassData.FIELD_NAME, Object[].class)
							.invokeExact(ctx.allClassData());
				} catch (Throwable e) {
					throw new AssertionError(e); // We just generated the field!
				}
				
				// Cache the constructor and actual function MHs
				// They'll hold references to the underlying class
				var jvmUpvalueTypes = Arrays.stream(upvalueTypes)
						.map(LuaType::backingType)
						.map(Type::loadedClass)
						.toArray(Class[]::new);
				var constructor = LOOKUP.findConstructor(implClass,
						MethodType.methodType(void.class, jvmUpvalueTypes));
				
				var normalArgCount = function.type().acceptedArgs().size();
				if (function.type().isVarargs()) {
					normalArgCount--; // Last argument slot occupied by varargs multival
				}
				var jvmArgTypes = new ArrayList<Class<?>>();
				jvmArgTypes.add(Object.class); // Function instance
				jvmArgTypes.addAll(Arrays.stream(argTypes)
						.limit(normalArgCount)
						.map(LuaType::backingType)
						.map(Type::loadedClass)
						.toList());
				if (function.type().isVarargs()) {
					jvmArgTypes.add(Object[].class); // Varargs array
				}
				
				var jvmReturnType = ctx.returnType().equals(LuaType.NIL)
						? Type.VOID : ctx.returnType().backingType();
				var method = LOOKUP.findVirtual(implClass, function.type().functionName(),
						MethodType.methodType(jvmReturnType.loadedClass(), jvmArgTypes));
				
				return new CompiledFunction(constructor, method);
			} catch (IllegalAccessException | NoSuchMethodException e) {
				throw new AssertionError(e);
			}
		});
		
		// Create new instance of the function with these upvalues
		// Bind the instance to returned method handle
		try {
			var instance = compiledFunc.constructor().invokeWithArguments(function.upvalues());
			var target = compiledFunc.function().bindTo(instance);
			if (function.type().isVarargs()) {
				// Make sure we can actually accept varargs
				target = target.asVarargsCollector(Object[].class);
			}
			return target;
		} catch (Throwable e) {
			throw new AssertionError(e);
		}
	}
	
	private static byte[] generateCode(LuaContext ctx, LuaType.Function type,
			LuaType[] argTypes, LuaType[] upvalueTypes) {
		// Create class that wraps the method acting as function body
		var def = ClassDef.create(toClassName(type.moduleName()), Access.PUBLIC);
		def.sourceFile(type.moduleName());
		
		// Class data constants
		def.addStaticField(Access.PUBLIC, Type.OBJECT.array(1), ClassData.FIELD_NAME);
		
		// Add fields for upvalues
		for (var i = 0; i < upvalueTypes.length; i++) {
			var template = type.upvalues().get(i);
			// If (and only if) types of upvalue templates are known, they must also match runtime types
			assert template.type().equals(LuaType.UNKNOWN) || template.type().equals(upvalueTypes[i]);
			def.addInstanceField(Access.PUBLIC, upvalueTypes[i].backingType(),
					template.variable().name(), FieldFlag.SYNTHETIC);
		}
		
		// Create constructor that puts upvalues to fields
		var constructor = def.addConstructor(Access.PUBLIC, MethodFlag.SYNTHETIC);
		constructor.add(constructor.self().callPrivate(Type.OBJECT, Type.VOID, "<init>"));
		for (var i = 0; i < upvalueTypes.length; i++) {
			var template = type.upvalues().get(i);
			var arg = constructor.arg(upvalueTypes[i].backingType());
			constructor.add(constructor.self().putField(template.variable().name(), arg));
		}
		constructor.add(Return.nothing());
		
		// Generate method contents
		var jvmReturnType = ctx.returnType().equals(LuaType.NIL)
				? Type.VOID : ctx.returnType().backingType();
		var method = def.addMethod(jvmReturnType, type.functionName(), Access.PUBLIC);
		
		// Add the LuaFunction ('self') argument
		// Currently unused, but easier to drop it here than with MethodHandles
		method.arg(Type.OBJECT);
		
		// Add function arguments to the method
		var acceptedArgs = type.acceptedArgs();
		var normalArgCount = type.isVarargs() ? acceptedArgs.size() - 1 : acceptedArgs.size();
		for (var i = 0; i < Math.min(argTypes.length, normalArgCount); i++) {
			var luaVar = acceptedArgs.get(i);
			var arg = method.mutableArg(argTypes[i].backingType(), luaVar.name());
			ctx.addFunctionArg(luaVar, arg);
		}
		if (argTypes.length < normalArgCount) {
			// Too few arguments, make the rest nil
			for (var i = argTypes.length; i < acceptedArgs.size(); i++) {
				var missingArg = Variable.create(Type.OBJECT);
				method.add(missingArg.set(Constant.nullValue(Type.OBJECT)));
				ctx.addFunctionArg(acceptedArgs.get(i), missingArg);
			}
		}
		if (type.isVarargs()) {
			// Accept any number of trailing arguments contained in Object[]
			var arg = method.mutableArg(Type.OBJECT.array(1), "...");
			ctx.addFunctionArg(LuaLocalVar.VARARGS, arg);
		}
		// Call site will ignore extraneous arguments
		
		// Read upvalues from fields to local variables
		for (var i = 0; i < upvalueTypes.length; i++) {
			var template = type.upvalues().get(i);
			var value = method.add(template.variable().name(), method.self()
					.getField(upvalueTypes[i].backingType(), template.variable().name()));
			ctx.addFunctionArg(template.variable(), value);
		}
		
		// Emit Lua code as JVM bytecode
		type.body().emit(ctx, method.block());
		
		return def.compile(); // Delegate to code4jvm for compilation
	}
	
	private static String toClassName(String moduleName) {
		// Of course, there is no guarantee that it actually is a path
		// But if it is, we'd best use platform path handling to strip the unnecessary parts
		var path = Path.of(moduleName);
		var fileName = path.getFileName().toString();
		return fileName.endsWith(".lua") ? fileName.substring(0, fileName.length() - 4) : fileName;
	}

}
