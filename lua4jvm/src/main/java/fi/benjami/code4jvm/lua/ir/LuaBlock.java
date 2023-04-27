package fi.benjami.code4jvm.lua.ir;

import java.util.List;

import fi.benjami.code4jvm.Value;
import fi.benjami.code4jvm.block.Block;
import fi.benjami.code4jvm.lua.compiler.LuaContext;

public record LuaBlock(
		List<IrNode> nodes
) implements IrNode {

	@Override
	public Value emit(LuaContext ctx, Block block) {
		for (var node : nodes) {
			node.emit(ctx, block);
		}
		return null;
	}

	@Override
	public LuaType outputType(LuaContext ctx) {
		var returnTypes = ctx.returnTypes();
		if (returnTypes.length == 0) {
			return LuaType.NIL;
		} else if (returnTypes.length == 1) {
			return returnTypes[0];
		} else {
			return LuaType.tuple(returnTypes);
		}
	}
	
}
