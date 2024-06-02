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
import fi.benjami.code4jvm.lua.linker.CallSiteOptions;
import fi.benjami.code4jvm.lua.linker.LuaLinker;
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
			var table = tableField.table().emit(ctx, block);
			var field = tableField.field().emit(ctx, block);
			if (tableField.field() instanceof LuaConstant constant) {				
				// Use invokedynamic w/ LuaLinker to try to speed up reads
				var options = new CallSiteOptions(new LuaType[] {LuaType.UNKNOWN, LuaType.UNKNOWN, LuaType.UNKNOWN}, false, false);
				var bootstrap = LuaLinker.BOOTSTRAP_DYNAMIC.withCapturedArgs(ctx.addClassData(options));
				// FIXME code4jvm bug: calling a dynamic target tries to infer types from values, even though it has explicit argTypes available!
				var getter = ctx.addClassData(TableAccess.CONSTANT_GET, Type.OBJECT);
				var target = CallTarget.dynamic(bootstrap, Type.OBJECT, "_",
						Type.OBJECT, Type.OBJECT, Type.OBJECT);
				return block.add(target.call(getter, table.asType(Type.OBJECT), field));
			} else {
				// Just get directly; since we can't cache slot, the guards would just slow down reads
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
