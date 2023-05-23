package fi.benjami.code4jvm.lua.ir.expr;

import java.util.List;

import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.Value;
import fi.benjami.code4jvm.block.Block;
import fi.benjami.code4jvm.lua.compiler.LuaContext;
import fi.benjami.code4jvm.lua.ir.IrNode;
import fi.benjami.code4jvm.lua.ir.LuaType;
import fi.benjami.code4jvm.statement.StringConcat;

public record StringConcatExpr(
		List<IrNode> parts
) implements IrNode {

	@Override
	public Value emit(LuaContext ctx, Block block) {
		return block.add(StringConcat.concat(parts.stream()
				.map(part -> part.emit(ctx, block))
				.map(value -> value.cast(Type.STRING))
				.toArray(Value[]::new)));
	}

	@Override
	public LuaType outputType(LuaContext ctx) {
		return LuaType.STRING; // TODO metatables
	}
}
