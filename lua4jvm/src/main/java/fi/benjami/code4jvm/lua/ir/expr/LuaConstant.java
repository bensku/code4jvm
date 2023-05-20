package fi.benjami.code4jvm.lua.ir.expr;

import fi.benjami.code4jvm.Value;
import fi.benjami.code4jvm.block.Block;
import fi.benjami.code4jvm.lua.compiler.LuaContext;
import fi.benjami.code4jvm.lua.ir.IrNode;
import fi.benjami.code4jvm.lua.ir.LuaType;

/**
 * An expression that loads a constant (i.e. any Java object or primitive).
 *
 */
public record LuaConstant(
		Object value,
		LuaType type
) implements IrNode {

	@Override
	public Value emit(LuaContext ctx, Block block) {
		// TODO use non-dynamic constants when possible
		return ctx.addClassData(value);
	}

	@Override
	public LuaType outputType(LuaContext ctx) {
		return type;
	}

}
