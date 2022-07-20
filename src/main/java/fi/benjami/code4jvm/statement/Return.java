package fi.benjami.code4jvm.statement;

import fi.benjami.code4jvm.Statement;
import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.Value;
import fi.benjami.code4jvm.block.Block;
import fi.benjami.code4jvm.internal.LocalVar;

import static org.objectweb.asm.Opcodes.*;

public class Return implements Statement {

	private final Value value;
	
	private Return(Value value) {
		this.value = value;
	}
	
	public static Statement nothing() {
		return new Return(null);
	}
	
	public static Statement value(Value value) {
		if (value.type().equals(Type.VOID)) {
			return new Return(null);
		}
		return new Return(value);
	}

	@Override
	public void emitVoid(Block block) {
		var inputs = value != null ? new Value[] {value} : new Value[0];
		block.add(Bytecode.run(Type.VOID, inputs, (ctx, currentBlock) -> {
			// Nodes may be emitted in different block they were added to
			// e.g. when copy() is used
			var redirect = currentBlock.returnRedirect();
			var mv = ctx.asm();
			
			if (redirect == null) {
				if (value == null) {
					mv.visitInsn(RETURN);
				} else {
					mv.visitInsn(value.type().getOpcode(IRETURN, ctx));
				}
			} else {
				if (value != null) {
					var holder = redirect.valueHolder().orElse(null);
					if (holder != null) {
						// Copy to local variable declared for return redirect
						// Note that this needs special handling in FrameBuilder!
						var slot = ((LocalVar) holder).assignedSlot;
						assert slot != -1 : "FrameBuilder didn't assign slot for ReturnRedirect";
						mv.visitVarInsn(value.type().getOpcode(ISTORE, ctx), slot);
					}
				}
				
				// Usually, requesting label in bytecode generation phase is unsafe
				// FrameBuilder has made sure that this label actually exists
				mv.visitJumpInsn(GOTO, redirect.target().requestLabel(Jump.Target.START));
			}
		}));
	}
}
