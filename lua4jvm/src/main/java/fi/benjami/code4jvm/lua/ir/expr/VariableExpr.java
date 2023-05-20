package fi.benjami.code4jvm.lua.ir.expr;

import fi.benjami.code4jvm.Value;
import fi.benjami.code4jvm.block.Block;
import fi.benjami.code4jvm.lua.compiler.LuaContext;
import fi.benjami.code4jvm.lua.ir.IrNode;
import fi.benjami.code4jvm.lua.ir.LuaLocalVar;
import fi.benjami.code4jvm.lua.ir.LuaType;
import fi.benjami.code4jvm.lua.ir.LuaVariable;
import fi.benjami.code4jvm.lua.ir.TableField;

/**
 * Expression that reads current value of a variable.
 *
 */
public record VariableExpr(
		LuaVariable source
) implements IrNode {

	@Override
	public Value emit(LuaContext ctx, Block block) {
		if (source instanceof LuaLocalVar localVar) {
			return ctx.resolveLocalVar(localVar);
		} else if (source instanceof TableField tableField) {
			var table = tableField.table().emit(ctx, block);
			// TODO lua table get method
		}
		throw new AssertionError();
	}

	@Override
	public LuaType outputType(LuaContext ctx) {
		return ctx.variableType(source);
	}
	
}