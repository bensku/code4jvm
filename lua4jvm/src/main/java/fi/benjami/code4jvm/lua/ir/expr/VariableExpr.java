package fi.benjami.code4jvm.lua.ir.expr;

import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.Value;
import fi.benjami.code4jvm.block.Block;
import fi.benjami.code4jvm.call.CallTarget;
import fi.benjami.code4jvm.lua.compiler.LuaContext;
import fi.benjami.code4jvm.lua.ir.IrNode;
import fi.benjami.code4jvm.lua.ir.LuaLocalVar;
import fi.benjami.code4jvm.lua.ir.LuaType;
import fi.benjami.code4jvm.lua.ir.LuaVariable;
import fi.benjami.code4jvm.lua.ir.TableField;
import fi.benjami.code4jvm.lua.runtime.LuaLinker;
import fi.benjami.code4jvm.lua.runtime.TableAccess;

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
				// Use invokedynamic w/ LuaLinker to speed up metatable access
				var bootstrap = LuaLinker.BOOTSTRAP_DYNAMIC.withCapturedArgs(ctx.addClassData(new LuaType[] 
						{LuaType.UNKNOWN, LuaType.UNKNOWN, LuaType.UNKNOWN}));
				// FIXME code4jvm bug: calling a dynamic target tries to infer types from values, even though it has explicit argTypes available!
				var getter = ctx.addClassData(TableAccess.GET_TARGET).asType(Type.OBJECT);
				var table = tableField.table().emit(ctx, block).asType(Type.OBJECT); // TODO but this one is lua4jvm quirk :)
				var field = tableField.field().emit(ctx, block);
				var target = CallTarget.dynamic(bootstrap, Type.OBJECT, "_",
						Type.OBJECT, Type.OBJECT, Type.OBJECT);
				return block.add(target.call(getter, table, field));
			}
		}
		throw new AssertionError();
	}

	@Override
	public LuaType outputType(LuaContext ctx) {
		return ctx.variableType(source);
	}
	
}
