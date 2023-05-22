package fi.benjami.code4jvm.lua.compiler;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Stream;

import fi.benjami.code4jvm.Constant;
import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.Variable;
import fi.benjami.code4jvm.flag.Access;
import fi.benjami.code4jvm.flag.FieldFlag;
import fi.benjami.code4jvm.flag.MethodFlag;
import fi.benjami.code4jvm.lua.ir.LuaType;
import fi.benjami.code4jvm.lua.runtime.CallResolver;
import fi.benjami.code4jvm.lua.runtime.LuaFunction;
import fi.benjami.code4jvm.statement.Return;
import fi.benjami.code4jvm.typedef.ClassDef;

/**
 * Compiles specializations for {@link LuaFunction functions} and (in future)
 * other callables based on types of their upvalues and arguments.
 * 
 * @see CallResolver
 *
 */
public class FunctionCompiler {
	
	private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
	
	/**
	 * Fetches or compiles a specialization for given Lua function.
	 * @param argTypes Argument types to use for compilation.
	 * @param callable The Lua function object.
	 * @return Method handle that may be called.
	 */
	public static MethodHandle callTarget(LuaType[] argTypes, LuaFunction function) {
		var upvalueTypes = function.upvalueTypes();
		var cacheKey = new LuaType[argTypes.length + upvalueTypes.length];
		System.arraycopy(upvalueTypes, 0, cacheKey, 0, upvalueTypes.length);
		System.arraycopy(argTypes, 0, cacheKey, upvalueTypes.length, argTypes.length);
		
		// Compile and load the function code, or use something that is already cached
		var compiledFunc = function.type().specializations().computeIfAbsent(LuaType.tuple(cacheKey), t -> {
			var ctx = function.type().newContext(argTypes);
			var code = generateCode(ctx, function.type(), argTypes, upvalueTypes);
			try {
				Files.write(Path.of("Debug.class"), code);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			try {
				var lookup = LOOKUP.defineHiddenClassWithClassData(code, ctx.allClassData(), true);
				
				// Cache the constructor and actual function MHs
				// They'll hold references to the underlying class
				var jvmUpvalueTypes = Arrays.stream(upvalueTypes)
						.map(LuaType::backingType)
						.map(Type::loadedClass)
						.toArray(Class[]::new);
				var constructor = lookup.findConstructor(lookup.lookupClass(),
						MethodType.methodType(void.class, jvmUpvalueTypes));
				
				var jvmArgTypes = Stream.concat(Stream.of(Object.class), Arrays.stream(argTypes)
						.map(LuaType::backingType)
						.map(Type::loadedClass))
						.toArray(Class[]::new);
				var jvmReturnType = ctx.returnType().equals(LuaType.NIL)
						? Type.VOID : ctx.returnType().backingType();
				var method = LOOKUP.findVirtual(lookup.lookupClass(), "call",
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
			return compiledFunc.function().bindTo(instance);
		} catch (Throwable e) {
			throw new AssertionError(e);
		}
	}
	
	private static byte[] generateCode(LuaContext ctx, LuaType.Function type,
			LuaType[] argTypes, LuaType[] upvalueTypes) {
		// Create class that wraps the method acting as function body
		// TODO store name in function if available?
		var def = ClassDef.create("fi.benjami.code4jvm.lua.compiler.CompiledFunction", Access.PUBLIC);
		
		// Add fields for upvalues
		// Whoever loads the compiled class must set these (using e.g. reflection)
		for (var i = 0; i < upvalueTypes.length; i++) {
			var template = type.upvalues().get(i);
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
		var method = def.addMethod(jvmReturnType, "call", Access.PUBLIC);
		
		// Add the LuaFunction ('self') argument
		// Currently unused, but easier to drop it here than with MethodHandles
		method.arg(Type.OBJECT);
		
		// Add function arguments to the method
		// TODO varargs
		var acceptedArgs = type.acceptedArgs();
		for (var i = 0; i < Math.min(argTypes.length, acceptedArgs.size()); i++) {
			var luaVar = acceptedArgs.get(i);
			var arg = method.mutableArg(argTypes[i].backingType(), luaVar.name());
			ctx.addFunctionArg(luaVar, arg);
		}
		if (argTypes.length < acceptedArgs.size()) {
			// Too few arguments, make the rest nil
			for (var i = argTypes.length; i < acceptedArgs.size(); i++) {
				var missingArg = Variable.create(Type.OBJECT);
				method.add(missingArg.set(Constant.nullValue(Type.OBJECT)));
				ctx.addFunctionArg(acceptedArgs.get(i), missingArg);
			}
		} else {
			// Too many arguments; accept and never use them
			for (var i = acceptedArgs.size(); i < argTypes.length; i++) {
				method.arg(argTypes[i].backingType(), "_");
			}
		}
		
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

}
