package fi.benjami.code4jvm.internal;

import static org.objectweb.asm.Opcodes.ILOAD;

import fi.benjami.code4jvm.Constant;
import fi.benjami.code4jvm.Value;

public class ValueTools {

	public static void emitInput(MethodCompilerState state, Value input) {
		var mv = state.ctx().asm();
		var slotAllocator = state.slotAllocator();
		if (input instanceof Constant constant) {
			mv.visitLdcInsn(constant.value());
		} else if (input instanceof LocalVar localVar) {
			if (localVar == LocalVar.EMPTY_MARKER) {
				return; // Ignore and skip unknown input assert
			}
			if (localVar.needsSlot) {
				mv.visitVarInsn(localVar.type().getOpcode(ILOAD, state.ctx()), slotAllocator.get(localVar));
			} // else: already on stack
		} else if (input instanceof CastValue cast) {
			// Recursively emit the original, then the required cast
			emitInput(state, cast.original());
			cast.emitCast(mv);
		} else if (input instanceof StackTop) {
			// Do nothing, value is already on stack
		} else {
			throw new AssertionError("unknown input: " + input);
		}
	}
}
