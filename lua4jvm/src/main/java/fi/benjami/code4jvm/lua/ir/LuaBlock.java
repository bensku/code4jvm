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
		// Go through everything to record return types
		for (var node : nodes) {
			node.outputType(ctx);
		}
		
		return ctx.returnType();
	}
	
	@Override
	public boolean hasReturn() {
		// The return might be inside a sub-block
		for (var node : nodes) {
			if (node.hasReturn()) {
				return true;
			}
		}
		return false;
	}
	
}
