package fi.benjami.code4jvm.lua.ir.stmt;

import fi.benjami.code4jvm.Value;
import fi.benjami.code4jvm.block.Block;
import fi.benjami.code4jvm.lua.compiler.LuaContext;
import fi.benjami.code4jvm.lua.ir.IrNode;
import fi.benjami.code4jvm.lua.ir.LuaType;

public record LoopBreakStmt(
		LoopStmt.LoopRef ref
) implements IrNode {

	@Override
	public Value emit(LuaContext ctx, Block block) {
		block.add(ref.breakLoop);
		return null;
	}

	@Override
	public LuaType outputType(LuaContext ctx) {
		return LuaType.NIL;
	}
}
