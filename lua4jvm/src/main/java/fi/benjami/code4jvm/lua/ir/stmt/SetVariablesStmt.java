package fi.benjami.code4jvm.lua.ir.stmt;

import java.util.List;

import fi.benjami.code4jvm.Constant;
import fi.benjami.code4jvm.Statement;
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
import fi.benjami.code4jvm.lua.ir.expr.FunctionCallExpr;
import fi.benjami.code4jvm.lua.ir.expr.VariableExpr;
import fi.benjami.code4jvm.lua.runtime.LuaBox;
import fi.benjami.code4jvm.lua.runtime.LuaTable;
import fi.benjami.code4jvm.lua.runtime.MultiVals;

/**
 * Statement that sets one or more variables.
 *
 */
public record SetVariablesStmt(
		/**
		 * Variables to set.
		 */
		List<? extends LuaVariable> targets,
		
		/**
		 * Expressions that produce the values for variables.
		 */
		List<IrNode> sources,
		
		/**
		 * Whether or not the value returned by the last source should
		 * by spread over rest of the targets. Earlier sources are never
		 * spread; instead, first value of each Lua multival is taken.
		 * 
		 * <p>Only function calls and explicit '...' can return multivalues,
		 * so this is false for all other sources.
		 */
		boolean spread
) implements IrNode {
	
	public SetVariablesStmt {
		assert targets.size() >= 1;
	}
	
	public SetVariablesStmt(List<? extends LuaVariable> targets, List<IrNode> sources) {
		this(targets, sources, MultiVals.canReturnMultiVal(sources.get(sources.size() - 1)));
	}

	@Override
	public Value emit(LuaContext ctx, Block block) {
		// Read sources that can't ever by multivals and write them to targets
		// (why can't they be multivals? we tell LuaLinker to take care of that)
		var normalSources = spread ? sources.size() - 1 : sources.size();
		for (var i = 0; i < Math.min(normalSources, targets.size()); i++) {
			var value = sources.get(i).emit(ctx, block);
			block.add(setVariable(ctx, targets.get(i), value));
		}
		
		// Emit sources that don't have targets; they might have side effects!
		for (var i = targets.size(); i < normalSources; i++) {
			sources.get(i).emit(ctx, block);
		}
		
		if (spread) {
			// TODO optimize this based on compile-time types
			// (this is not as critical as it used to be, because spread is usually false for non multivals)
			// Spread the last source over remaining targets
			ctx.setAllowSpread(true);
			var multiVal = sources.get(sources.size() - 1).emit(ctx, block).cast(Type.OBJECT);
			ctx.setAllowSpread(false);
			
			// Extract the first value or use the value as-is if it isn't multi-val
			var value = block.add(SPREAD_FIRST.call(multiVal));
			block.add(setVariable(ctx, targets.get(normalSources), value));
			// Extract the rest of values, using nulls if they're not present
			for (var i = normalSources + 1; i < targets.size(); i++) {
				value = block.add(SPREAD_REST.call(multiVal, Constant.of(i - normalSources)));
				block.add(setVariable(ctx, targets.get(i), value));
			}
		} else {
			// If there are leftover targets, set them to nil
			for (var i = normalSources; i < targets.size(); i++) {
				block.add(setVariable(ctx, targets.get(i), Constant.nullValue(Type.OBJECT)));
			}
		}
		
		return null;
	}
	
	private Statement setVariable(LuaContext ctx, LuaVariable variable, Value value) {
		return block -> {
			if (variable instanceof LuaLocalVar localVar) {
				if (localVar.upvalue() && localVar.mutable()) {
					// Mutable upvalues need to be put to LuaBoxes
					if (!ctx.hasBeenAssigned(localVar)) {
						// First assignment? Initialize box!
						var box = block.add(LuaBox.TYPE.newInstance());
						block.add(ctx.resolveLocalVar(localVar).set(box));
					}
					block.add(ctx.resolveLocalVar(localVar).putField("value", value.cast(Type.OBJECT)));
				} else {
					// Normal local variable assignment
					var jvmVar = ctx.resolveLocalVar(localVar);
					block.add(jvmVar.set(value.cast(jvmVar.type())));
				}
			} else if (variable instanceof TableField tableField) {
				// Just call the setter
				// TODO invokedynamic to TableAccess.CONSTANT_SET once it has some optimizations
				var table = tableField.table().emit(ctx, block).cast(LuaTable.TYPE);
				var field = tableField.field().emit(ctx, block).cast(Type.OBJECT);
				block.add(table.callVirtual(Type.VOID, "set", field, value.cast(Type.OBJECT)));
			} else {				
				throw new AssertionError();
			}
		};
	}

	@Override
	public LuaType outputType(LuaContext ctx) {
		var normalSources = spread ? sources.size() - 1 : sources.size();
		for (var i = 0; i < Math.min(normalSources, targets.size()); i++) {
			var target = targets.get(i);
			ctx.recordType(target, sources.get(i).outputType(ctx));
			target.markMutable();
		}
		
		if (spread) {
			// Spread the last source over remaining targets
			ctx.setAllowSpread(true);
			var multiValType = sources.get(sources.size() - 1).outputType(ctx);
			ctx.setAllowSpread(false);
			for (var i = normalSources; i < targets.size(); i++) {
				// TODO fix the typing, so that multiValType ==
				// Tuple -> types for individual variables
				// UNKNOWN -> current behavior
				// anything else -> first multiValType, rest NIL
				var target = targets.get(i);
				ctx.recordType(target, LuaType.UNKNOWN);
				target.markMutable();
			}
		} else {
			// If there are leftover targets, set them to nil
			for (var i = normalSources; i < targets.size(); i++) {
				var target = targets.get(i);
				ctx.recordType(target, LuaType.NIL);
				target.markMutable();
			}
		}
		return LuaType.NIL;
	}
	
	private static final CallTarget SPREAD_FIRST = Type.of(SetVariablesStmt.class)
			.staticMethod(Type.OBJECT, "spreadFirst", Type.OBJECT);
	
	public static Object spreadFirst(Object value) {
		if (value instanceof Object[] array) {
			return array.length != 0 ? array[0] : null;
		}
		return value;
	}
	
	private static final CallTarget SPREAD_REST = Type.of(SetVariablesStmt.class)
			.staticMethod(Type.OBJECT, "spreadRest", Type.OBJECT, Type.INT);
	
	public static Object spreadRest(Object value, int index) {
		return value instanceof Object[] array && array.length > index ? array[index] : null;
	}

}
