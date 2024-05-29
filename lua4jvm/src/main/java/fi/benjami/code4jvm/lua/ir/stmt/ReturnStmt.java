package fi.benjami.code4jvm.lua.ir.stmt;

import java.util.ArrayList;
import java.util.List;

import fi.benjami.code4jvm.Constant;
import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.Value;
import fi.benjami.code4jvm.Variable;
import fi.benjami.code4jvm.block.Block;
import fi.benjami.code4jvm.lua.compiler.LuaContext;
import fi.benjami.code4jvm.lua.ir.IrNode;
import fi.benjami.code4jvm.lua.ir.LuaType;
import fi.benjami.code4jvm.lua.runtime.MultiVals;
import fi.benjami.code4jvm.statement.Arithmetic;
import fi.benjami.code4jvm.statement.ArrayAccess;
import fi.benjami.code4jvm.statement.Return;

public record ReturnStmt(
		List<IrNode> values
) implements IrNode {

	@Override
	public Value emit(LuaContext ctx, Block block) {
		if (values.isEmpty()) {
			if (ctx.returnType().equals(LuaType.NIL)) {				
				block.add(Return.nothing());
			} else {
				block.add(Return.value(Constant.nullValue(ctx.returnType().backingType())));
			}
		} else if (values.size() == 1 && !MultiVals.canReturnMultiVal(values.get(0))) {
			var value = values.get(0);
			if (value.outputType(ctx).equals(LuaType.NIL)) {
				block.add(Return.nothing());
			} else {
				block.add(Return.value(value.emit(ctx, block).cast(ctx.returnType().backingType())));
			}
		} else if (ctx.truncateReturn()) {
			// Multiple return values, but only one is used
			// The rest must still be emitted due to potential side effects
			var first = values.get(0).emit(ctx, block).cast(ctx.returnType().backingType());
			for (var i = 0; i < values.size(); i++) {
				values.get(i).emit(ctx, block);
			}
			block.add(Return.value(first));
		} else {
			// TODO specialized tuples
			if (!MultiVals.canReturnMultiVal(values.get(values.size() - 1))) {
				// Last cannot be multival
				var tuple = block.add(Variable.create(Type.OBJECT.array(1)),
						Type.OBJECT.array(1).newInstance(Constant.of(values.size())));
				for (var i = 0; i < values.size(); i++) {
					block.add(ArrayAccess.set(tuple, Constant.of(i),
							values.get(i).emit(ctx, block).cast(Type.OBJECT)));
				}
				
				block.add(Return.value(tuple));
			} else {
				// Potential multival; need to be prepared for it
				var localVars = new ArrayList<Value>();
				for (var i = 0; i < values.size() - 1; i++) {
					localVars.add(values.get(i).emit(ctx, block));
				}
				// Last value MIGHT spread
				ctx.setAllowSpread(true);
				var last = values.get(values.size() - 1).emit(ctx, block).cast(Type.OBJECT);
				ctx.setAllowSpread(false);
				
				// Now we know how many return values there are!
				var multiValLen = block.add(MultiVals.ARRAY_LENGTH.call(last));
				var resultCount = block.add(Arithmetic.add(multiValLen, Constant.of(values.size() - 1)));
				var tuple = block.add(Variable.create(Type.OBJECT.array(1)),
						Type.OBJECT.array(1).newInstance(resultCount));
				
				// Put the values to array
				for (var i = 0; i < values.size() - 1; i++) {
					block.add(ArrayAccess.set(tuple, Constant.of(i), localVars.get(i).cast(Type.OBJECT)));
				}
				// ... including the one or more multival values
				block.add(MultiVals.EXTEND_ARRAY.call(tuple, Constant.of(values.size() - 1), last));
				
				block.add(Return.value(tuple));
			}
		}
		return null;
	}

	@Override
	public LuaType outputType(LuaContext ctx) {
		if (values.isEmpty()) {
			ctx.returnTypes(LuaType.NIL);
		} else if ((values.size() == 1 && !MultiVals.canReturnMultiVal(values.get(0)))
				|| ctx.truncateReturn()) {
			ctx.returnTypes(values.get(0).outputType(ctx));			
		} else {
			// TODO specialized tuples - here too?
			if (!MultiVals.canReturnMultiVal(values.get(values.size() - 1))) {
				// We're returning a multival, but last value of it is not a multival
				ctx.returnTypes(values.stream()
						.map(value -> value.outputType(ctx))
						.toArray(LuaType[]::new));
			} else {
				// We are returning a multival, to which another multival might be appended
				// Now, this is a bit tricky...
				var returnTypes = new ArrayList<LuaType>();
				for (var i = 0; i < values.size() - 1; i++) {
					returnTypes.add(values.get(i).outputType(ctx));
				}
				// Last value MIGHT spread
				ctx.setAllowSpread(true);
				values.get(values.size() - 1).outputType(ctx); // Let the type analysis work
				returnTypes.add(LuaType.UNKNOWN); // TODO specialized tuples - especially here
				ctx.setAllowSpread(false);
				
				ctx.returnTypes(returnTypes.toArray(LuaType[]::new));
			}
		}
		return LuaType.NIL;
	}
	
	@Override
	public boolean hasReturn() {
		return true;
	}
}
