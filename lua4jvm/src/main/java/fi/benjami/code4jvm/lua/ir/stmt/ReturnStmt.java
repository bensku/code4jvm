package fi.benjami.code4jvm.lua.ir.stmt;

import fi.benjami.code4jvm.Value;
import fi.benjami.code4jvm.block.Block;
import fi.benjami.code4jvm.lua.compiler.LuaContext;
import fi.benjami.code4jvm.lua.ir.IrNode;
import fi.benjami.code4jvm.lua.ir.LuaType;
import fi.benjami.code4jvm.statement.Return;

public record ReturnStmt(
		IrNode value
) implements IrNode {

	@Override
	public Value emit(LuaContext ctx, Block block) {
		if (value == null) {
			block.add(Return.nothing());
		} else {
			block.add(Return.value(value.emit(ctx, block)));
		}
		return null;
	}

	@Override
	public LuaType outputType(LuaContext ctx) {
		if (value == null) {
			ctx.returnTypes(LuaType.NIL);
		} else {			
			var type = value.outputType(ctx);
			if (type instanceof LuaType.Tuple tuple) {
				ctx.returnTypes(tuple.types());
			} else {
				ctx.returnTypes(type);
			}
		}
		return LuaType.NIL;
	}
}
