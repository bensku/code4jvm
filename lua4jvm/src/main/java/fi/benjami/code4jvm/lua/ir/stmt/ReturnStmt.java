package fi.benjami.code4jvm.lua.ir.stmt;

import java.util.List;

import fi.benjami.code4jvm.Constant;
import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.Value;
import fi.benjami.code4jvm.block.Block;
import fi.benjami.code4jvm.lua.compiler.LuaContext;
import fi.benjami.code4jvm.lua.ir.IrNode;
import fi.benjami.code4jvm.lua.ir.LuaType;
import fi.benjami.code4jvm.statement.ArrayAccess;
import fi.benjami.code4jvm.statement.Return;

public record ReturnStmt(
		List<IrNode> values
) implements IrNode {

	@Override
	public Value emit(LuaContext ctx, Block block) {
		if (values.isEmpty()) {
			block.add(Return.nothing());
		} else if (values.size() == 1) {
			var value = values.get(0);
			if (value.outputType(ctx).equals(LuaType.NIL)) {
				block.add(Return.nothing());
			} else {				
				block.add(Return.value(values.get(0).emit(ctx, block)));
			}
		} else {
			// TODO specialized tuples
			var tuple = block.add(Type.OBJECT.array(1).newInstance(Constant.of(values.size())));
			for (var i = 0; i < values.size(); i++) {
				block.add(ArrayAccess.set(tuple, Constant.of(i),
						values.get(i).emit(ctx, block).cast(Type.OBJECT)));
			}
			block.add(Return.value(tuple));
		}
		return null;
	}

	@Override
	public LuaType outputType(LuaContext ctx) {
		if (values.isEmpty()) {
			ctx.returnTypes(LuaType.NIL);
		} else {
			ctx.returnTypes(values.stream()
					.map(value -> value.outputType(ctx))
					.toArray(LuaType[]::new));
		}
		return LuaType.NIL;
	}
	
	@Override
	public boolean hasReturn() {
		return true;
	}
}
