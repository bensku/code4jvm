package fi.benjami.code4jvm.lua.ir.expr;

import java.util.HashMap;
import java.util.HashSet;
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
			if (entry.key() instanceof LuaConstant field
					&& type.knownEntries().containsKey(field.value())) {
				// Fast path: constant key, directly set the field
				var key = field.value();
				var value = entry.value.emit(ctx, block).cast(Type.OBJECT);
				block.add(table.putField("_" + key, value));
			} else {
				// Key is not constant, call set method
				var key = entry.key.emit(ctx, block).cast(Type.OBJECT);
				var value = entry.value.emit(ctx, block).cast(Type.OBJECT); // TODO emit after or before key?
				block.add(table.callVirtual(Type.VOID, "set", key, value));
			}
			
		}
		return table;
	}

	@Override
	public LuaType outputType(LuaContext ctx) {
		if (ctx.getCache(this) instanceof LuaType cached) {
			return cached;
		}
		
		var entryMap = new HashMap<String, LuaType>();
		var knownTypes = new HashSet<LuaType>();
		var initializeMap = false;
		for (var entry : entries) {
			if (entry.key instanceof LuaConstant constant && constant.value() instanceof String str) {
				// Known key, add it to shape
				entryMap.put(str, entry.value.outputType(ctx));
			} else {
				initializeMap = true; // We know writing to the map will be needed
			}
			// Record type even if key is not constant
			knownTypes.add(entry.value.outputType(ctx));
		}
		// NOTE: caching the shape is crucial so that we can use amendments made to it in emit phase!
		return ctx.cached(this, LuaType.shape(entryMap, knownTypes, initializeMap));
	}
}
