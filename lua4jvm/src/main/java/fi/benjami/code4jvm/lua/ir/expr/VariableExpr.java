package fi.benjami.code4jvm.lua.ir.expr;

import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.Value;
import fi.benjami.code4jvm.block.Block;
import fi.benjami.code4jvm.lua.compiler.LuaContext;
import fi.benjami.code4jvm.lua.ir.IrNode;
import fi.benjami.code4jvm.lua.ir.LuaLocalVar;
import fi.benjami.code4jvm.lua.ir.LuaType;
import fi.benjami.code4jvm.lua.ir.LuaVariable;
import fi.benjami.code4jvm.lua.ir.TableField;
import fi.benjami.code4jvm.lua.runtime.LuaTable;

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
			var tableType = tableField.table().outputType(ctx);
			if (tableType instanceof LuaType.Shape shape
					&& tableField.field() instanceof LuaConstant field
					&& shape.compiledForm().includedKeys().contains(field.value())) {
				var table = tableField.table().emit(ctx, block);
				return block.add(table.getField(Type.OBJECT, "_" + field.value()));
			} else {				
				var table = tableField.table().emit(ctx, block).cast(LuaTable.TYPE);
				var field = tableField.field().emit(ctx, block);
				return block.add(table.callVirtual(Type.OBJECT, "get", field.cast(Type.OBJECT)));
			}
		}
		throw new AssertionError();
	}

	@Override
	public LuaType outputType(LuaContext ctx) {
		return ctx.variableType(source);
	}
	
}
