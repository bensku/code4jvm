package fi.benjami.code4jvm.lua.ir.expr;

import java.util.List;

import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.Value;
import fi.benjami.code4jvm.block.Block;
import fi.benjami.code4jvm.lua.compiler.LuaContext;
import fi.benjami.code4jvm.lua.ir.IrNode;
import fi.benjami.code4jvm.lua.ir.LuaType;

public record TableInitExpr(
		List<Entry> entries
) implements IrNode {

	public record Entry(IrNode key, IrNode value) {}

	@Override
	public Value emit(LuaContext ctx, Block block) {
		var type = (LuaType.Shape) ctx.getCache(this);
		
		var shapeType = type.backingType();
		var table = block.add(shapeType.newInstance());
		for (var entry : entries) {
			// Call setRaw(), because this can't yet have a metatable!
			// TODO give all tables from this initializer same shape (invokedynamic magic)
			var key = entry.key.emit(ctx, block).cast(Type.OBJECT);
			var value = entry.value.emit(ctx, block).cast(Type.OBJECT); // TODO emit after or before key?
			block.add(table.callVirtual(Type.VOID, "setRaw", key, value));
			
		}
		return table;
	}

	@Override
	public LuaType outputType(LuaContext ctx) {
		// TODO reconsider if any of this is needed - shapes as they were planned didn't work out
		if (ctx.getCache(this) instanceof LuaType cached) {
			return cached;
		}
		var shape = LuaType.shape();
		for (var entry : entries) {
			if (entry.key instanceof LuaConstant constant && constant.value() instanceof String str) {
				// Known key, add it to shape
				shape.amend(str, entry.value.outputType(ctx));
			} else {
				shape.amendUnknown();
			}
			// Record type even if key is not constant
			// TODO we don't currently need this information
		}
		// NOTE: caching the shape is crucial so that we can use amendments made to it in emit phase!
		return ctx.cached(this, shape);
	}
}
