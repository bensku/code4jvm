package fi.benjami.code4jvm.lua.ir.stmt;

import java.util.List;

import fi.benjami.code4jvm.Condition;
import fi.benjami.code4jvm.Constant;
import fi.benjami.code4jvm.Statement;
import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.Value;
import fi.benjami.code4jvm.block.Block;
import fi.benjami.code4jvm.lua.compiler.LuaContext;
import fi.benjami.code4jvm.lua.ir.IrNode;
import fi.benjami.code4jvm.lua.ir.LuaLocalVar;
import fi.benjami.code4jvm.lua.ir.LuaType;
import fi.benjami.code4jvm.lua.ir.LuaVariable;
import fi.benjami.code4jvm.lua.ir.TableField;
import fi.benjami.code4jvm.statement.ArrayAccess;
import fi.benjami.code4jvm.statement.Jump;
import fi.benjami.code4jvm.structure.IfBlock;

/**
 * Statement that sets one or more variables.
 *
 */
public record SetVariablesStmt(
		/**
		 * Variables to set.
		 */
		List<LuaVariable> targets,
		
		/**
		 * Expressions that produce the values for variables.
		 */
		List<IrNode> sources,
		
		/**
		 * Whether or not the value returned by first source should be spread
		 * over all targets. This is normally done for function calls only.
		 */
		boolean spread
) implements IrNode {
	
	public SetVariablesStmt {
		assert targets.size() >= 1;
		assert !spread || sources.size() == 1;
		assert sources.size() >= 1;
	}

	@Override
	public Value emit(LuaContext ctx, Block block) {
		// TODO is calling outputType twice expensive?
		if (spread) {
			// One source spread to multiple targets
			// This is tricky, especially if we have unknown types (i.e. no help from static analysis)
			var type = sources.get(0).outputType(ctx);
			var value = sources.get(0).emit(ctx, block);
			if (type instanceof LuaType.Tuple tuple) {
				// The source is an array, copy values from there to variables
				for (var i = 0; i < Math.min(tuple.types().length, targets.size()); i++) {
					var source = block.add(ArrayAccess.get(value, Constant.of(i)));
					block.add(setVariable(ctx, targets.get(i), source));
				}
				// Set rest of them null (Lua type nil)
				for (var i = tuple.types().length; i < targets.size(); i++) {
					block.add(setVariable(ctx, targets.get(i), Constant.nullValue(Type.OBJECT)));
				}
			} else if (type.equals(LuaType.UNKNOWN)) {
				// We need to check whether or not the source is an array
				// TODO specialized tuples need special handling here
				var arrayTest = new IfBlock();
				arrayTest.branch(nested -> {
					var c = nested.add(value.callVirtual(Type.of(Class.class), "getClass"));
					var isArray = nested.add(c.callVirtual(Type.BOOLEAN, "isArray"));
					return Condition.isTrue(isArray);
				}, nested -> {
					var array = value.cast(Type.OBJECT.array(1));
					// Function returned multiple values (but we don't know how many!)
					// First, write nulls to all variables
					// This probably has a performance hit, but this is slow path anyway
					for (var i = 0; i < targets.size(); i++) {
						nested.add(setVariable(ctx, targets.get(i), Constant.nullValue(Type.OBJECT)));
					}
					
					// Then, overwrite nulls with whatever the function returned
					var length = nested.add(ArrayAccess.length(array));
					for (var i = 0; i < targets.size(); i++) {
						// If the array has no more elements, leave the rest as nulls
						nested.add(Jump.to(nested, Jump.Target.END, Condition.greaterOrEqual(Constant.of(i), length)));
						
						var source = nested.add(ArrayAccess.get(array, Constant.of(i)));
						nested.add(setVariable(ctx, targets.get(i), source));
					}
				});
				arrayTest.fallback(nested -> {
					// Function returned one value, write it and leave rest as nulls
					nested.add(setVariable(ctx, targets.get(0), value));
					for (var i = 1; i < targets.size(); i++) {
						nested.add(setVariable(ctx, targets.get(i), Constant.nullValue(Type.OBJECT)));
					}
				});
				block.add(arrayTest);
			} else {
				// The source is known not to be an array
				block.add(setVariable(ctx, targets.get(0), value));
				for (var i = 1; i < targets.size(); i++) {
					block.add(setVariable(ctx, targets.get(0), Constant.nullValue(Type.OBJECT)));
				}
			}
		} else {
			// One source per target (this makes everything easier)
			// Set values from existing sources to targets
			for (var i = 0; i < Math.min(sources.size(), targets.size()); i++) {
				var type = sources.get(i).outputType(ctx);
				var value = sources.get(i).emit(ctx, block);
				var target = targets.get(i);
				if (type instanceof LuaType.Tuple tuple) {
					// The source is an array, take the first element of it
					var firstElement = block.add(ArrayAccess.get(value, Constant.of(0)));
					block.add(setVariable(ctx, target, firstElement));
				} else if (type.equals(LuaType.UNKNOWN)) {
					// Check whether or not the source is an array
					// E.g. return values of unknown functions take this path
					// TODO specialized tuples need special handling here
					var arrayTest = new IfBlock();
					arrayTest.branch(nested -> {
						var c = nested.add(value.callVirtual(Type.of(Class.class), "getClass"));
						var isArray = nested.add(c.callVirtual(Type.BOOLEAN, "isArray"));
						return Condition.isTrue(isArray);
					}, nested -> {
						// It is, take first element only
						var firstElement = nested.add(ArrayAccess.get(value.cast(Type.OBJECT.array(1)), Constant.of(0)));
						nested.add(setVariable(ctx, target, firstElement));
					});
					arrayTest.fallback(nested -> {
						// Not array, use value as-is
						nested.add(setVariable(ctx, target, value));
					});
					block.add(arrayTest);
				} else {
					// The source is known not to be an array, use it as-is
					block.add(setVariable(ctx, target, value));
				}
			}
			
			// Emit, but ignore outputs of, rest of sources
			for (var i = targets.size() - 1; i < sources.size(); i++) {
				sources.get(i).emit(ctx, block);
			}
			
			// Set targets without sources to null/nil
			for (var i = sources.size() - 1; i < targets.size(); i++) {
				setVariable(ctx, targets.get(i), Constant.nullValue(Type.OBJECT));
			}
		}
		return null; // Statements don't have outputs
	}
	
	private Statement setVariable(LuaContext ctx, LuaVariable variable, Value value) {
		return block -> {			
			if (variable instanceof LuaLocalVar localVar) {
				var jvmVar = ctx.resolveLocalVar(localVar);
				block.add(jvmVar.set(value.cast(jvmVar.type())));
			} else if (variable instanceof TableField tableField) {
				var table = tableField.table().emit(ctx, block);
				// TODO lua table set method
			} else {				
				throw new AssertionError();
			}
		};
	}

	@Override
	public LuaType outputType(LuaContext ctx) {
		if (spread) {
			var type = sources.get(0).outputType(ctx);
			if (type instanceof LuaType.Tuple tuple) {
				// Function WILL return multiple values, spread them to targets
				for (var i = 0; i < Math.min(tuple.types().length, targets.size()); i++) {
					ctx.recordType(targets.get(i), tuple.types()[i]);
				}
				// Rest of the target variables are nil
				for (var i = tuple.types().length - 1; i < targets.size(); i++) {
					ctx.recordType(targets.get(i), LuaType.NIL);
				}
			} else if (type.equals(LuaType.UNKNOWN)) {
				// Function might or might not return multiple values
				// All targets are unknown (potentially nil)
				for (var i = 0; i < targets.size(); i++) {
					ctx.recordType(targets.get(i), LuaType.UNKNOWN);
				}
			} else {
				// Function returns only one value, put it to first target
				ctx.recordType(targets.get(0), type);
				// Rest of target variables are nil
				for (var i = 1; i < targets.size(); i++) {
					ctx.recordType(targets.get(i), LuaType.NIL);
				}
			}
		} else {
			// One source = one target
			for (var i = 0; i < Math.min(sources.size(), targets.size()); i++) {
				var type = sources.get(i).outputType(ctx);
				if (type instanceof LuaType.Tuple tuple) {
					type = tuple.types()[0]; // Take first result, ignore rest
				}
				ctx.recordType(targets.get(i), type);
			}			
		}
		return LuaType.NIL;
	}
}
