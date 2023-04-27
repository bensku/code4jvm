package fi.benjami.code4jvm.lua.compiler;

import fi.benjami.code4jvm.Constant;
import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.Variable;
import fi.benjami.code4jvm.flag.Access;
import fi.benjami.code4jvm.flag.FieldFlag;
import fi.benjami.code4jvm.lua.ir.LuaType;
import fi.benjami.code4jvm.lua.runtime.LuaFunction;
import fi.benjami.code4jvm.typedef.ClassDef;

public class FunctionCompiler {

	public byte[] compile(LuaFunction function, LuaType... argTypes) {
		// Init scope with upvalue types
		var ctx = new LuaContext();
		for (var entry : function.upvalues().entrySet()) {
			ctx.recordType(entry.getKey(), LuaType.of(entry.getValue().getClass()));
		}
		
		// Add types of function arguments
		// Missing arguments are allowed and treated as nil
		for (var i = 0; i < function.args().size(); i++) {
			if (argTypes.length > i) {				
				ctx.recordType(function.args().get(i), argTypes[i]);
			} else {
				ctx.recordType(function.args().get(i), LuaType.NIL);
			}
		}
		
		// Compute types of local variables and the return type
		var returnType = function.body().outputType(ctx);
		
		// Create class that wraps the method acting as function body
		var def = ClassDef.create("lua4jvm.CompiledFunction", Access.PUBLIC);
		
		// Add fields for upvalues
		// Whoever loads the compiled class must set these (using e.g. reflection)
		for (var entry : function.upvalues().entrySet()) {
			def.addStaticField(Access.PUBLIC, Type.of(entry.getValue().getClass()), entry.getKey().name(), FieldFlag.SYNTHETIC);
		}
		
		// Generate method contents
		var method = def.addMethod(returnType.backingType(), "call", Access.PUBLIC);
		
		// Add function arguments to the method
		// TODO varargs
		for (var i = 0; i < Math.min(argTypes.length, function.args().size()); i++) {
			var luaVar = function.args().get(i);
			var arg = method.mutableArg(argTypes[i].backingType(), luaVar.name());
			ctx.addFunctionArg(luaVar, arg);
		}
		if (argTypes.length < function.args().size()) {
			// Too few arguments, make the rest nil
			for (var i = argTypes.length; i < function.args().size(); i++) {				
				var missingArg = Variable.create(Type.OBJECT);
				method.add(missingArg.set(Constant.nullValue(Type.OBJECT)));
				ctx.addFunctionArg(function.args().get(i), missingArg);
			}
		} else {
			// Too many arguments; accept and never use them
			for (var i = function.args().size(); i < argTypes.length; i++) {
				method.arg(argTypes[i].backingType(), "_");
			}
		}
		
		// Read upvalues from fields to local variables
		for (var entry : function.upvalues().entrySet()) {
			var value = method.add(entry.getKey().name(), def.type()
					.getStatic(Type.of(entry.getValue().getClass()), entry.getKey().name()));
			ctx.addFunctionArg(entry.getKey(), value);
		}
		
		// Emit function body
		function.body().emit(ctx, method.block());
		
		return def.compile();
	}
}
