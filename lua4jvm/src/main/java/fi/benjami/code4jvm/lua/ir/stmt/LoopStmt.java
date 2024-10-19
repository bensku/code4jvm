package fi.benjami.code4jvm.lua.ir.stmt;

import fi.benjami.code4jvm.Condition;
import fi.benjami.code4jvm.Value;
import fi.benjami.code4jvm.block.Block;
import fi.benjami.code4jvm.lua.compiler.LoopRef;
import fi.benjami.code4jvm.lua.compiler.LuaContext;
import fi.benjami.code4jvm.lua.ir.IrNode;
import fi.benjami.code4jvm.lua.ir.LuaBlock;
import fi.benjami.code4jvm.lua.ir.LuaType;
import fi.benjami.code4jvm.statement.Jump;

public record LoopStmt(
		IrNode condition,
		LuaBlock body,
		Kind kind,
		LoopRef ref
) implements IrNode {
	
	public enum Kind {
		WHILE,
		REPEAT_UNTIL
	}
	
	public LoopStmt {
		// TODO invariant checks for kind
	}

	@Override
	public Value emit(LuaContext ctx, Block block) {
		// TODO code4jvm's LoopBlock is dangerously useless; return here if/when it is fixed
		var loop = Block.create();
		if (kind == Kind.WHILE) {
			var cond = condition.emit(ctx, loop);
			loop.add(Jump.to(loop, Jump.Target.END, Condition.isFalse(cond)));
		}
		
		// Tell LoopBreaks how to break out of this loop
		ref.breakLoop = b -> b.add(Jump.to(loop, Jump.Target.END));
		body.emit(ctx, loop);
		ref.breakLoop = null;
		
		if (kind == Kind.REPEAT_UNTIL) {
			var cond = condition.emit(ctx, loop);
			loop.add(Jump.to(loop, Jump.Target.START, Condition.isFalse(cond)));
		} else {
			loop.add(Jump.to(loop, Jump.Target.START));
		}
		
		block.add(loop);
		return null;
	}

	@Override
	public LuaType outputType(LuaContext ctx) {
		return LuaType.NIL;
	}
	
	@Override
	public boolean hasReturn() {
		// If there is condition before first run, it might not return
		return kind != Kind.REPEAT_UNTIL ? false : body.hasReturn();
	}
	
	@Override
	public void flagVariables(LuaContext ctx) {
		condition.flagVariables(ctx);
		body.flagVariables(ctx);
	}
}
