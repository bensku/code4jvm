package fi.benjami.code4jvm.lua.ir.expr;

import java.util.Arrays;
import java.util.List;

import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.Value;
import fi.benjami.code4jvm.block.Block;
import fi.benjami.code4jvm.call.CallTarget;
import fi.benjami.code4jvm.call.FixedCallTarget;
import fi.benjami.code4jvm.lua.compiler.LuaContext;
import fi.benjami.code4jvm.lua.ir.IrNode;
import fi.benjami.code4jvm.lua.ir.LuaLocalVar;
import fi.benjami.code4jvm.lua.ir.LuaType;
import fi.benjami.code4jvm.lua.linker.CallSiteOptions;
import fi.benjami.code4jvm.lua.linker.LuaLinker;
import fi.benjami.code4jvm.lua.runtime.MultiVals;

public record FunctionCallExpr(
		IrNode function,
		List<IrNode> args
) implements IrNode {

	private record CachedCall(LuaType[] argTypes, LuaType returnType) {}
	
	@Override
	public Value emit(LuaContext ctx, Block block) {
		return emit(ctx, block, null);
	}
	
	public Value emit(LuaContext ctx, Block block, String intrinsicId) {
		// Get expensive-to-compute types from cache
		var cache = (CachedCall) ctx.getCache(this);
		var argTypes = cache.argTypes();
		var returnType = cache.returnType();
		
		// TODO constant bootstrap is broken due to upvalues
//		FixedCallTarget bootstrap;
//		if (function instanceof VariableExpr variable // function is a variable read
//				&& variable.source() instanceof LuaLocalVar localVar // from local variable
//				&& localVar.upvalue() && !localVar.mutable() // that will be stable between calls to this function
//				&& TODO we also need to check that 1) linker has used LuaType.TARGET_HAS_CHANGED (to prove upvalue's block hasn't been re-executed)
//				&& TODO 2) cache key includes identity of the upvalue, not just its type! (this is quite tricky)
//				) {
//			
//		}
//		
		var bootstrap = LuaLinker.BOOTSTRAP_DYNAMIC;
		var lastMultiVal = !args.isEmpty() && MultiVals.canReturnMultiVal(args.get(args.size() - 1));
		var options = new CallSiteOptions(ctx.owner(), argTypes, ctx.allowSpread(), lastMultiVal, intrinsicId);
		bootstrap = bootstrap.withCapturedArgs(ctx.addClassData(options));
		
		// Evaluate arguments to values (function is first argument)
		var argValues = new Value[args.size() + 1];
		argValues[0] = function.emit(ctx, block).asType(Type.OBJECT);
		for (var i = 0; i < args.size() - 1; i++) {
			argValues[i + 1] = args.get(i).emit(ctx, block);
		}
		if (!args.isEmpty()) {
			// The last argument may spread (if it is the kind that can return a multival)
			ctx.setAllowSpread(true);
			argValues[argValues.length - 1] = args.get(args.size() - 1).emit(ctx, block);
			ctx.setAllowSpread(false);
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
		var argTypes = args.stream()
				.map(node -> node.outputType(ctx))
				.toArray(LuaType[]::new);
		// TODO multival type analysis
		if (type instanceof LuaType.Function function) {
			// Analyze types of arguments in this call
			// Run type analysis for the entire function to figure out the return type
			var returnType = LuaContext.forFunction(ctx.owner(), function, !ctx.allowSpread(), argTypes).returnType();
			ctx.cached(this, new CachedCall(argTypes, returnType));
			return returnType;
		} else {
			// Types of tables/shapes are gone if they are passed to unknown function
			// (the function could write to them or even modify the metatable!)
			for (var argType : argTypes) {
				if (argType instanceof LuaType.Shape shape) {
					shape.types().escapedAnalysis();
				}
			}
			ctx.cached(this, new CachedCall(argTypes, LuaType.UNKNOWN));
			return LuaType.UNKNOWN;
		}
	}
}
