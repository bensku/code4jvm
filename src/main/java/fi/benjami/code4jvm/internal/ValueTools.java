package fi.benjami.code4jvm.internal;

import static org.objectweb.asm.Opcodes.*;

import org.objectweb.asm.MethodVisitor;

import fi.benjami.code4jvm.Constant;
import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.Value;
import fi.benjami.code4jvm.util.TypeUtils;

public class ValueTools {

	public static void emitInput(MethodCompilerState state, Value input) {
		var mv = state.ctx().asm();
		var slotAllocator = state.slotAllocator();
		if (input instanceof Constant constant) {
			emitConstant(mv, constant);
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
	
	private static void emitConstant(MethodVisitor mv, Constant constant) {
		var type = constant.type();
		if (type.equals(Type.BOOLEAN)) {
			var value = (boolean) constant.value();
			// true == 1, false == 0
			mv.visitInsn(value ? ICONST_1 : ICONST_0);
		} else if (TypeUtils.isIntLike(type)) {
			var value = (int) constant.value();
			switch (value) {
			case -1 -> mv.visitInsn(ICONST_M1);
			case 0 -> mv.visitInsn(ICONST_0);
			case 1 -> mv.visitInsn(ICONST_1);
			case 2 -> mv.visitInsn(ICONST_2);
			case 3 -> mv.visitInsn(ICONST_3);
			case 4 -> mv.visitInsn(ICONST_4);
			case 5 -> mv.visitInsn(ICONST_5);
			default -> {
				if ((value & 0xff) == 0) {
					// Fits to byte
					mv.visitIntInsn(BIPUSH, value);
				} else if ((value & 0xffff) == 0) {
					// Fits to short
					mv.visitIntInsn(SIPUSH, value);
				} else {
					// Need to add to constant table
					mv.visitLdcInsn(value);
				}
			}
			}
		} else if (type.equals(Type.FLOAT)) {
			var value = (float) constant.value();
			if (value == 0) {
				mv.visitInsn(FCONST_0);
			} else if (value == 1) {
				mv.visitInsn(FCONST_1);
			} else if (value == 2) {
				mv.visitInsn(FCONST_2);
			} else {
				mv.visitLdcInsn(value);
			}
		} else if (type.equals(Type.LONG)) {
			var value = (long) constant.value();
			if (value == 0) {
				mv.visitInsn(LCONST_0);
			} else if (value == 1) {
				mv.visitInsn(LCONST_1);
			} else {
				mv.visitLdcInsn(value);
			}
		} else if (type.equals(Type.DOUBLE)) {
			var value = (double) constant.value();
			if (value == 0) {
				mv.visitInsn(DCONST_0);
			} else if (value == 1) {
				mv.visitInsn(DCONST_1);
			} else {
				mv.visitLdcInsn(value);
			}
		} else {
			mv.visitLdcInsn(constant.asmValue());
		}
	}
}
