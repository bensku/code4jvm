package fi.benjami.code4jvm.lua.ir.expr;

import fi.benjami.code4jvm.Condition;
import fi.benjami.code4jvm.Constant;
import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.Value;
import fi.benjami.code4jvm.Variable;
import fi.benjami.code4jvm.block.Block;
import fi.benjami.code4jvm.lua.compiler.LuaContext;
import fi.benjami.code4jvm.lua.ir.IrNode;
import fi.benjami.code4jvm.lua.ir.LuaType;
import fi.benjami.code4jvm.statement.Jump;

public record LogicalExpr(
		IrNode lhs,
		Kind kind,
		IrNode rhs
) implements IrNode {

	public enum Kind {
		AND,
		OR
	}

	@Override
	public Value emit(LuaContext ctx, Block block) {
		// TODO metatables
		var lhsValue = lhs.emit(ctx, block).cast(Type.BOOLEAN);
		
		// TODO code4jvm support for chaining conditions, this is ugly
		
		var result = Variable.create(Type.BOOLEAN);
		var testBlock = Block.create();
		if (kind == Kind.AND) {
			testBlock.add(result.set(Constant.of(false)));
			testBlock.add(Jump.to(testBlock, Jump.Target.END, Condition.isFalse(lhsValue)));
			var rhsValue = rhs.emit(ctx, testBlock).cast(Type.BOOLEAN);
			testBlock.add(Jump.to(testBlock, Jump.Target.END, Condition.isFalse(rhsValue)));
			testBlock.add(result.set(Constant.of(true)));
		} else {
			testBlock.add(result.set(Constant.of(true)));
			testBlock.add(Jump.to(testBlock, Jump.Target.END, Condition.isTrue(lhsValue)));
			var rhsValue = rhs.emit(ctx, testBlock).cast(Type.BOOLEAN);
			testBlock.add(Jump.to(testBlock, Jump.Target.END, Condition.isTrue(rhsValue)));
			testBlock.add(result.set(Constant.of(false)));
		}
		
		block.add(testBlock);
		return result;
	}

	@Override
	public LuaType outputType(LuaContext ctx) {
		return LuaType.BOOLEAN; // TODO metatables
	}
}
