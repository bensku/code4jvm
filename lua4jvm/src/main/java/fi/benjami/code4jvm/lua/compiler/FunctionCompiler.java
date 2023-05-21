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
import fi.benjami.code4jvm.lua.ir.UpvalueTemplate;
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
	 * Fetches or compiles a specialization for given callable.
	 * @param argTypes Argument types to use for compilation.
	 * @param callable The callable object, e.g. a function.
	 * @return Method handle that may be called.
	 */
	public static MethodHandle callTarget(LuaType[] argTypes, Object callable) {
		if (callable instanceof LuaFunction function) {
			// Compile and load the function code, or use something that is already cached
			var compiledFunc = function.type().specializations().computeIfAbsent(LuaType.tuple(argTypes), t -> {
				var ctx = function.type().newContext(argTypes);
				var code = generateCode(ctx, function.type(), argTypes);
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
					var jvmUpvalueTypes = Arrays.stream(function.type().upvalues())
							.map(UpvalueTemplate::type)
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
		} else {
			// TODO revisit with metatables
			throw new UnsupportedOperationException(callable + " is not callable");
		}
	}
	
	private static byte[] generateCode(LuaContext ctx, LuaType.Function type, LuaType[] argTypes) {
		// Compute types of local variables and the return type
		var returnType = type.body().outputType(ctx);
		
		// Create class that wraps the method acting as function body
		// TODO store name in function if available?
		var def = ClassDef.create("fi.benjami.code4jvm.lua.compiler.CompiledFunction", Access.PUBLIC);
		
		// Add fields for upvalues
		// Whoever loads the compiled class must set these (using e.g. reflection)
		for (var upvalue : type.upvalues()) {
			def.addInstanceField(Access.PUBLIC, upvalue.type().backingType(), upvalue.variable().name(), FieldFlag.SYNTHETIC);
		}
		
		// Create constructor that puts upvalues to fields
		var constructor = def.addConstructor(Access.PUBLIC, MethodFlag.SYNTHETIC);
		constructor.add(constructor.self().callPrivate(Type.OBJECT, Type.VOID, "<init>"));
		for (var upvalue : type.upvalues()) {
			var arg = constructor.arg(upvalue.type().backingType());
			constructor.add(constructor.self().putField(upvalue.variable().name(), arg));
		}
		constructor.add(Return.nothing());
		
		// Generate method contents
		var jvmReturnType = returnType.equals(LuaType.NIL) ? Type.VOID : returnType.backingType();
		var method = def.addMethod(jvmReturnType, "call", Access.PUBLIC);
		
		// Add the LuaFunction ('self') argument
		// Currently unused, but easier to drop it here than with MethodHandles
		method.arg(Type.OBJECT);
		
		// Add function arguments to the method
		// TODO varargs
		var acceptedArgs = type.acceptedArgs();
		for (var i = 0; i < Math.min(argTypes.length, acceptedArgs.length); i++) {
			var luaVar = acceptedArgs[i];
			var arg = method.mutableArg(argTypes[i].backingType(), luaVar.name());
			ctx.addFunctionArg(luaVar, arg);
		}
		if (argTypes.length < acceptedArgs.length) {
			// Too few arguments, make the rest nil
			for (var i = argTypes.length; i < acceptedArgs.length; i++) {				
				var missingArg = Variable.create(Type.OBJECT);
				method.add(missingArg.set(Constant.nullValue(Type.OBJECT)));
				ctx.addFunctionArg(acceptedArgs[i], missingArg);
			}
		} else {
			// Too many arguments; accept and never use them
			for (var i = acceptedArgs.length; i < argTypes.length; i++) {
				method.arg(argTypes[i].backingType(), "_");
			}
		}
		
		// Read upvalues from fields to local variables
		for (var upvalue : type.upvalues()) {
			var value = method.add(upvalue.variable().name(), method.self()
					.getField(upvalue.type().backingType(), upvalue.variable().name()));
			ctx.addFunctionArg(upvalue.variable(), value);
		}
		
		// Emit Lua code as JVM bytecode
		type.body().emit(ctx, method.block());
		
		return def.compile(); // Delegate to code4jvm for compilation
	}

}
