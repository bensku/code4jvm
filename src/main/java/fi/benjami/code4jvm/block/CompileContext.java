package fi.benjami.code4jvm.block;

import static org.objectweb.asm.Opcodes.*;

import org.objectweb.asm.MethodVisitor;

import fi.benjami.code4jvm.Constant;
import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.Value;
import fi.benjami.code4jvm.call.FixedCallTarget;
import fi.benjami.code4jvm.config.CompileOptions;
import fi.benjami.code4jvm.internal.CastValue;
import fi.benjami.code4jvm.internal.LocalVar;
import fi.benjami.code4jvm.internal.StackTop;
import fi.benjami.code4jvm.typedef.ClassDef;
import fi.benjami.code4jvm.util.TypeUtils;

/**
 * Context for compiling a single method.
 *
 */
public record CompileContext(
		/**
		 * Type that is being compiled.
		 */
		ClassDef owner,
		
		/**
		 * Call target that refers to the method that is being compiled.
		 */
		FixedCallTarget method,
		
		/**
		 * ASM visitor used for emitting method bytecode.
		 */
		MethodVisitor asm,
		
		/**
		 * Compiler options used for the class that the method is part of.
		 */
		CompileOptions options
) {
	
	public void loadExplicit(Value... inputs) {
		for (var input : inputs) {
			loadExplicit(input);
		}
	}
	
	public void loadExplicit(Value input) {
		if (input instanceof Constant constant) {
			emitConstant(asm, constant);
		} else if (input instanceof LocalVar localVar) {
			if (localVar == LocalVar.EMPTY_MARKER) {
				return; // Ignore and skip unknown input assert
			}
			if (localVar.needsSlot) {
				asm.visitVarInsn(localVar.type().getOpcode(ILOAD, this), localVar.assignedSlot);
			} // else: already on stack
		} else if (input instanceof CastValue cast) {
			// Recursively emit the original, then the required cast
			loadExplicit(cast.original());
			cast.emitCast(asm);
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
			int value;
			if (type.equals(Type.CHAR)) {
				// Although char is int for JVM, Character is not a Number in Java
				value = ((Character) constant.value()).charValue();
			} else {
				// Although byte and short are just ints for JVM, the boxed types
				// are not directly related and CANNOT be cast to each other
				value = ((Number) constant.value()).intValue();
			}
			switch (value) {
			case -1 -> mv.visitInsn(ICONST_M1);
			case 0 -> mv.visitInsn(ICONST_0);
			case 1 -> mv.visitInsn(ICONST_1);
			case 2 -> mv.visitInsn(ICONST_2);
			case 3 -> mv.visitInsn(ICONST_3);
			case 4 -> mv.visitInsn(ICONST_4);
			case 5 -> mv.visitInsn(ICONST_5);
			default -> {
				if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
					// Fits to byte
					mv.visitIntInsn(BIPUSH, value);
				} else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
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
