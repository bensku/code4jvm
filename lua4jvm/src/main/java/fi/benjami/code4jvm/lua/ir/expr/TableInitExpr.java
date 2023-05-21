package fi.benjami.code4jvm.lua.ir.expr;

import java.util.List;

import fi.benjami.code4jvm.Constant;
import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.Value;
import fi.benjami.code4jvm.block.Block;
import fi.benjami.code4jvm.lua.compiler.LuaContext;
import fi.benjami.code4jvm.lua.ir.IrNode;
import fi.benjami.code4jvm.lua.ir.LuaType;
import fi.benjami.code4jvm.lua.runtime.LuaTable;

public record TableInitExpr(
		List<Entry> entries
) implements IrNode {

	public record Entry(IrNode key, IrNode value) {}

	@Override
	public Value emit(LuaContext ctx, Block block) {
		// TODO should empty constructor create zero-sized table by default?
		var table = block.add(LuaTable.TYPE.newInstance(Constant.of(entries.size())));
		for (var entry : entries) {
			var key = entry.key.emit(ctx, block).cast(Type.OBJECT);
			var value = entry.value.emit(ctx, block).cast(Type.OBJECT);
			block.add(table.callVirtual(Type.VOID, "set", key, value));
		}
		return table;
	}

	@Override
	public LuaType outputType(LuaContext ctx) {
		return LuaType.TABLE;
	}
}
