package fi.benjami.code4jvm.lua.ir.expr;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;

import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.Value;
import fi.benjami.code4jvm.block.Block;
import fi.benjami.code4jvm.lua.compiler.LuaContext;
import fi.benjami.code4jvm.lua.ir.IrNode;
import fi.benjami.code4jvm.lua.ir.LuaType;
import fi.benjami.code4jvm.lua.linker.BinaryOp;
import fi.benjami.code4jvm.lua.linker.CallSiteOptions;
import fi.benjami.code4jvm.lua.linker.DynamicTarget;
import fi.benjami.code4jvm.lua.linker.LuaLinker;
import fi.benjami.code4jvm.lua.stdlib.LuaException;
import fi.benjami.code4jvm.statement.StringConcat;

public record StringConcatExpr(
		List<IrNode> parts
) implements IrNode {
	
	private static final DynamicTarget TARGET;
	private static final MethodHandle CONCAT_TWO;
	
	static {
		// TODO when nested concatenations are lowered, consider directly using StringConcatFactory
		var lookup = MethodHandles.lookup();
		try {
			CONCAT_TWO = lookup.findStatic(StringConcatExpr.class, "concat",
					MethodType.methodType(String.class, Object.class, String.class, String.class));
		} catch (NoSuchMethodException | IllegalAccessException e) {
			throw new AssertionError();
		}
		
		TARGET = BinaryOp.newTarget(String.class, CONCAT_TWO, "__concat",
				(a, b) -> new LuaException("attempted to concatenate non-string values"));
	}
	
	@SuppressWarnings("unused") // MethodHandle
	private static String concat(Object callable, String a, String b) {
		return a + b;
	}
	
	public StringConcatExpr {
		// TODO lower nested string concatenations to one operation
		assert parts.size() == 2; // For now, we don't support this
	}

	@Override
	public Value emit(LuaContext ctx, Block block) {
		if (outputType(ctx).equals(LuaType.STRING)) {
			// Only strings, concatenate directly
			return block.add(StringConcat.concat(parts.stream()
					.map(part -> part.emit(ctx, block))
					.map(value -> value.cast(Type.STRING))
					.toArray(Value[]::new)));
		} else {
			// Types not known at compile time; use invokedynamic
			var lhs = parts.get(0).emit(ctx, block);
			var rhs = parts.get(1).emit(ctx, block);
			return block.add(LuaLinker.setupCall(ctx, CallSiteOptions.nonFunction(ctx.owner(), LuaType.UNKNOWN, LuaType.UNKNOWN), TARGET, lhs, rhs));
		}
	}

	@Override
	public LuaType outputType(LuaContext ctx) {
		var nonStrings = parts.stream().map(part -> part.outputType(ctx))
				.anyMatch(type -> !type.equals(LuaType.STRING));
		return nonStrings ? LuaType.UNKNOWN : LuaType.STRING;
	}
}
