package fi.benjami.code4jvm.lua.ir.expr;

import java.util.function.BiFunction;

import fi.benjami.code4jvm.Condition;
import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.Value;
import fi.benjami.code4jvm.block.Block;
import fi.benjami.code4jvm.lua.compiler.LuaContext;
import fi.benjami.code4jvm.lua.ir.IrNode;
import fi.benjami.code4jvm.lua.ir.LuaType;

public record CompareExpr(
		IrNode lhs,
		Kind kind,
		IrNode rhs		
) implements IrNode {

	public enum Kind {
		LESS_THAN(Condition::lessThan),
		MORE_THAN(Condition::greaterThan),
		LESS_OR_EQUAL(Condition::lessOrEqual),
		MORE_OR_EQUAL(Condition::greaterOrEqual),
		NOT_EQUAL((lhs, rhs) -> Condition.equal(lhs, rhs).not()),
		EQUAL(Condition::equal);
		
		private BiFunction<Value, Value, Condition> conditionSupplier;
		
		private Kind(BiFunction<Value, Value, Condition> conditionSupplier) {
			this.conditionSupplier = conditionSupplier;
		}
	}

	@Override
	public Value emit(LuaContext ctx, Block block) {
		var lhsValue = lhs.emit(ctx, block);
		var rhsValue = rhs.emit(ctx, block);
		if (kind == Kind.EQUAL || kind == Kind.NOT_EQUAL) {
//			if (!lhsValue.type().equals(rhsValue.type())) {
				lhsValue = lhsValue.cast(Type.OBJECT);
				rhsValue = rhsValue.cast(Type.OBJECT);
//			}
		} else {
			// TODO unnecessary casts AND incompatibility with metatables
			lhsValue = lhsValue.cast(Type.DOUBLE);
			rhsValue = rhsValue.cast(Type.DOUBLE);
		}
		return block.add(kind.conditionSupplier.apply(lhsValue, rhsValue).evaluate());
	}

	@Override
	public LuaType outputType(LuaContext ctx) {
		// TODO metatables will break this assumption
		return LuaType.BOOLEAN;
	}
}
