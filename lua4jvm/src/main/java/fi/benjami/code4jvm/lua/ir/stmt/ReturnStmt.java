package fi.benjami.code4jvm.lua.ir.stmt;

import java.util.Arrays;

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
		IrNode[] values
) implements IrNode {

	@Override
	public Value emit(LuaContext ctx, Block block) {
		if (values.length == 0) {
			block.add(Return.nothing());
		} else if (values.length == 1) {
			block.add(Return.value(values[0].emit(ctx, block)));
		} else {
			// TODO specialized tuples
			var tuple = block.add(Type.OBJECT.array(1).newInstance(Constant.of(values.length)));
			for (var i = 0; i < values.length; i++) {
				block.add(ArrayAccess.set(tuple, Constant.of(i), values[i].emit(ctx, block).cast(Type.OBJECT)));
			}
			block.add(Return.value(tuple));
		}
		return null;
	}

	@Override
	public LuaType outputType(LuaContext ctx) {
		if (values.length == 0) {
			ctx.returnTypes(LuaType.NIL);
		} else {
			ctx.returnTypes(Arrays.stream(values)
					.map(value -> value.outputType(ctx))
					.toArray(LuaType[]::new));
		}
		return LuaType.NIL;
	}
}
