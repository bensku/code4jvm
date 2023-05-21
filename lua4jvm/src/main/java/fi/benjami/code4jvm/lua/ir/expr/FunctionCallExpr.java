package fi.benjami.code4jvm.lua.ir.expr;

import java.util.Arrays;
import java.util.List;

import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.Value;
import fi.benjami.code4jvm.block.Block;
import fi.benjami.code4jvm.call.CallTarget;
import fi.benjami.code4jvm.lua.compiler.LuaContext;
import fi.benjami.code4jvm.lua.ir.IrNode;
import fi.benjami.code4jvm.lua.ir.LuaType;
import fi.benjami.code4jvm.lua.runtime.CallResolver;

public record FunctionCallExpr(
		IrNode function,
		List<IrNode> args
) implements IrNode {

	@Override
	public Value emit(LuaContext ctx, Block block) {
		// TODO try to cache these
		var funcType = function.outputType(ctx);
		var argTypes = args.stream()
				.map(node -> node.outputType(ctx))
				.toArray(LuaType[]::new);
		var returnType = outputType(ctx);
		
		// TODO constant bootstrap is broken due to upvalues
		var bootstrap = CallResolver.BOOTSTRAP_DYNAMIC;
		bootstrap = bootstrap.withCapturedArgs(ctx.addClassData(argTypes));
		
		// Evaluate arguments to values (function is first argument)
		var argValues = new Value[args.size() + 1];
		argValues[0] = function.emit(ctx, block).asType(Type.OBJECT);
		for (var i = 0; i < args.size(); i++) {
			argValues[i + 1] = args.get(i).emit(ctx, block);
		}
		
		// Call the function using invokedynamic
		// Code will be generated now if needed
		var jvmReturnType = returnType.equals(LuaType.NIL) ? Type.VOID : returnType.backingType();
		var target = CallTarget.dynamic(bootstrap, jvmReturnType, "_",
				Arrays.stream(argTypes).map(LuaType::backingType).toArray(Type[]::new));
		return block.add(target.call(argValues));
	}

	@Override
	public LuaType outputType(LuaContext ctx) {
		var type = function.outputType(ctx);
		if (type instanceof LuaType.Function function) {
			// Analyze types of arguments in this call
			var argTypes = args.stream()
					.map(node -> node.outputType(ctx))
					.toArray(LuaType[]::new);
			// Run type analysis for the entire function to figure out the return type
			// TODO this is EXPENSIVE, results should be cached!
			return function.newContext(argTypes).returnType();
		} else {
			return LuaType.UNKNOWN;
		}
	}
}
