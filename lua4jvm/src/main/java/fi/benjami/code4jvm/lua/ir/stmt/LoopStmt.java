package fi.benjami.code4jvm.lua.ir.stmt;

import fi.benjami.code4jvm.Condition;
import fi.benjami.code4jvm.Statement;
import fi.benjami.code4jvm.Value;
import fi.benjami.code4jvm.block.Block;
import fi.benjami.code4jvm.lua.compiler.LuaContext;
import fi.benjami.code4jvm.lua.ir.IrNode;
import fi.benjami.code4jvm.lua.ir.LuaBlock;
import fi.benjami.code4jvm.lua.ir.LuaType;
import fi.benjami.code4jvm.statement.Jump;
import fi.benjami.code4jvm.structure.LoopBlock;

public record LoopStmt(
		IrNode condition,
		LuaBlock body,
		Kind kind,
		LoopRef ref
) implements IrNode {

	public static class LoopRef {
		public Statement breakLoop;
	}
	
	public enum Kind {
		WHILE,
		REPEAT_UNTIL
	}

	@Override
	public Value emit(LuaContext ctx, Block block) {
		// TODO code4jvm's LoopBlock is dangerously useless
		var loop = Block.create();
		if (kind == Kind.WHILE) {
			var cond = condition.emit(ctx, loop);
			loop.add(Jump.to(loop, Jump.Target.END, Condition.isFalse(cond)));
		}
		
		// Tell LoopBreaks how to break out of this loop
		ref.breakLoop = b -> b.add(Jump.to(loop, Jump.Target.END));
		body.emit(ctx, loop);
		ref.breakLoop = null;
		
		if (kind == Kind.WHILE) {
			loop.add(Jump.to(loop, Jump.Target.START));
		} else {
			var cond = condition.emit(ctx, loop);
			loop.add(Jump.to(loop, Jump.Target.START, Condition.isFalse(cond)));
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
		return kind == Kind.WHILE ? false : body.hasReturn();
	}
}
