package fi.benjami.code4jvm.internal;

import static org.objectweb.asm.Opcodes.*;

import fi.benjami.code4jvm.Value;

public record ReturnNode(
		/**
		 * Value to return or null to indicate void.
		 */
		Value value
) implements Node {
	
	public void emitBytecode(MethodCompilerState state, ReturnRedirect redirect) {
		var ctx = state.ctx();
		var mv = ctx.asm();
		var slotAllocator = state.slotAllocator();
		if (redirect == null) {
			// Normal return
			if (value == null) {
				// void return
				mv.visitInsn(RETURN);
			} else {
				// Return needs a value
				ValueTools.emitInput(state, value);
				mv.visitInsn(value.type().getOpcode(IRETURN, ctx));
			}
		} else {
			// Jump using redirect
			if (value != null) {
				// Copy to local variable declared for return redirect
				ValueTools.emitInput(state, value);
				mv.visitVarInsn(value.type().getOpcode(ISTORE, ctx),
						slotAllocator.get(redirect.returnValue()));
			}
			mv.visitJumpInsn(GOTO, redirect.target());
		}
	}
}
