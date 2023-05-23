package fi.benjami.code4jvm.lua.ir.stmt;

import java.util.List;

import fi.benjami.code4jvm.Condition;
import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.Value;
import fi.benjami.code4jvm.block.Block;
import fi.benjami.code4jvm.lua.compiler.LuaContext;
import fi.benjami.code4jvm.lua.ir.IrNode;
import fi.benjami.code4jvm.lua.ir.LuaBlock;
import fi.benjami.code4jvm.lua.ir.LuaType;
import fi.benjami.code4jvm.structure.IfBlock;

public record IfBlockStmt(
		List<Branch> branches,
		LuaBlock fallback
) implements IrNode {
	
	public record Branch(IrNode condition, LuaBlock body) {}

	@Override
	public Value emit(LuaContext ctx, Block block) {
		var ifBlock = new IfBlock();
		for (var branch : branches) {
			// TODO optimize to use all available conditions either here or in code4jvm
			ifBlock.branch(inner -> {
				var value = branch.condition.emit(ctx, inner).cast(Type.BOOLEAN);
				return Condition.isTrue(value);
			}, inner -> {
				branch.body.emit(ctx, inner);
			});
		}
		if (fallback != null) {
			ifBlock.fallback(inner -> fallback.emit(ctx, inner));
		}
		block.add(ifBlock);
		return null;
	}

	@Override
	public LuaType outputType(LuaContext ctx) {
		for (var branch : branches) {
			branch.body.outputType(ctx);
		}
		if (fallback != null) {
			fallback.outputType(ctx);
		}
		return LuaType.NIL;
	}
	
	@Override
	public boolean hasReturn() {
		if (branches.size() == 0 && fallback == null) {
			return false; // Nothing to return anything
		}
		
		// If even one branch doesn't return, then we don't unconditionally return
		for (var branch : branches) {
			if (!branch.body.hasReturn()) {
				return false;
			}
		}
		// If we don't have fallback, we might not return
		return fallback != null ? fallback.hasReturn() : false;
	}

}
