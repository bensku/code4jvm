package fi.benjami.code4jvm.lua.ir.expr;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import fi.benjami.code4jvm.Value;
import fi.benjami.code4jvm.block.Block;
import fi.benjami.code4jvm.lua.compiler.LuaContext;
import fi.benjami.code4jvm.lua.ir.IrNode;
import fi.benjami.code4jvm.lua.ir.LuaType;
import fi.benjami.code4jvm.lua.linker.CallSiteOptions;
import fi.benjami.code4jvm.lua.linker.DynamicTarget;
import fi.benjami.code4jvm.lua.linker.LuaLinker;
import fi.benjami.code4jvm.lua.linker.UnaryOp;
import fi.benjami.code4jvm.lua.stdlib.LuaException;
import fi.benjami.code4jvm.statement.Arithmetic;

public record NegateExpr(IrNode expr) implements IrNode {

	private static final MethodHandle NEGATE_DOUBLE, NEGATE_INT;
	private static final DynamicTarget TARGET;
	
	static {
		var lookup = MethodHandles.lookup();
		try {
			NEGATE_DOUBLE = lookup.findStatic(NegateExpr.class, "negate", MethodType.methodType(double.class, Object.class, double.class));
			NEGATE_INT = lookup.findStatic(NegateExpr.class, "negate", MethodType.methodType(int.class, Object.class, int.class));
		} catch (NoSuchMethodException | IllegalAccessException e) {
			throw new AssertionError(e);
		}
		
		TARGET = UnaryOp.newTarget(new UnaryOp.Path[] {
				new UnaryOp.Path(Double.class, NEGATE_DOUBLE),
				new UnaryOp.Path(Integer.class, NEGATE_INT)
		}, "__unm", (val) -> new LuaException("attempted to negate a non-number value"));
	}
	
	@SuppressWarnings("unused") // MethodHandle
	private static double negate(Object callable, double value) {
		return -value;
	}
	
	@SuppressWarnings("unused") // MethodHandle
	private static int negate(Object callable, int value) {
		return -value;
	}
	
	@Override
	public Value emit(LuaContext ctx, Block block) {
		var value = expr.emit(ctx, block);
		if (outputType(ctx).isNumber()) {
			return block.add(Arithmetic.negate(value));
		} else {
			return block.add(LuaLinker.setupCall(ctx, CallSiteOptions.nonFunction(ctx.owner(), LuaType.UNKNOWN, LuaType.UNKNOWN), TARGET, value));
		}
	}

	@Override
	public LuaType outputType(LuaContext ctx) {
		var exprType = expr.outputType(ctx);
		if (exprType == LuaType.INTEGER) {
			return LuaType.INTEGER;
		} else if (exprType == LuaType.FLOAT) {
			return LuaType.FLOAT;
		}
		// We can't do type analysis through metatables (yet)
		return LuaType.UNKNOWN;
	}

}
