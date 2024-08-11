package fi.benjami.code4jvm.lua.ir.expr;

import fi.benjami.code4jvm.Constant;
import fi.benjami.code4jvm.Type;
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

	public LuaConstant(Object value) {
		this(value, LuaType.of(value));
	}
	
	@Override
	public Value emit(LuaContext ctx, Block block) {
		if (value == null) {
			return Constant.nullValue(Type.OBJECT);
		} else if (value instanceof Boolean bool) {
			return Constant.of(bool);
		} else if (value instanceof Integer num) {
			return Constant.of(num);
		} else if (value instanceof Double num) {
			return Constant.of(num);
		} else if (value instanceof String str) {
			return Constant.of(str);
		} else {
			return ctx.addClassData(value);			
		}
	}

	@Override
	public LuaType outputType(LuaContext ctx) {
		return type;
	}

}
