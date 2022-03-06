package fi.benjami.code4jvm.statement;

import java.util.List;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import fi.benjami.code4jvm.Block;
import fi.benjami.code4jvm.Condition;
import fi.benjami.code4jvm.Statement;
import fi.benjami.code4jvm.internal.NeedsBlockLabels;
import fi.benjami.code4jvm.util.TypeUtils;

import static org.objectweb.asm.Opcodes.*;

public class Jump implements Statement, NeedsBlockLabels {
	
	public enum Target {
		START,
		END
	}

	public static Jump to(Block block, Target target, Condition condition) {
		return new Jump(block, target, condition);
	}
	
	public static Jump to(Block block, Target target) {
		return to(block, target, null);
	}
	
	private final Block block;
	private final Target target;
	private final Condition condition;
	
	private Label label;
	
	private Jump(Block block, Target target, Condition condition) {
		this.block = block;
		this.target = target;
		this.condition = condition;
	}
	
	@Override
	public Block targetBlock() {
		return block;
	}
	
	@Override
	public int setLabels(Label start, Label end) {
		label = target == Target.START ? start : end;
		return target == Target.START ? NeedsBlockLabels.NEED_START : NeedsBlockLabels.NEED_END;
	}

	@Override
	public void emitVoid(Block block) {
		if (condition != null) {
			var type = condition.values()[0].type();
			var isObject = type.getSort() == Type.OBJECT;
			var intLike = TypeUtils.isIntLike(type);
			block.add(Bytecode.run(Type.VOID_TYPE, List.of(condition.values()), mv -> {
				switch (condition.type()) {
				case REF_EQUAL -> mv.visitJumpInsn(IF_ACMPEQ, label);
				case REF_NOT_EQUAL -> mv.visitJumpInsn(IF_ACMPNE, label);
				// EQUAL and NOT_EQUAL can use many different bytecodes
				// - For objects, call Objects#equals(Object,Object)
				// (it calls Object#equals(Object) but is null-safe)
				// - For int-like types, use dedicated branching instructions
				// - For everything else, subtract and branch on zero
				case EQUAL -> {
					if (isObject) {
						objectsEquals(mv);
						mv.visitJumpInsn(IFEQ, label);
					} else if (intLike) {
						mv.visitJumpInsn(IF_ICMPEQ, label);
					} else {
						mv.visitInsn(type.getOpcode(ISUB)); // Zero if values are equal
						mv.visitJumpInsn(IFEQ, label);
					}
				}
				case NOT_EQUAL -> {
					if (isObject) {
						objectsEquals(mv);
						mv.visitJumpInsn(IFNE, label);
					} else if (intLike) {
						mv.visitJumpInsn(IF_ICMPNE, label);
					} else {
						mv.visitInsn(type.getOpcode(ISUB));
						mv.visitJumpInsn(IFNE, label);
					}
				}
				// Order comparisons also have many possible bytecodes
				// - Comparable#compareTo(Object) is called for objects
				// (this does NOT support nulls, but oh well)
				// - For int-like types, continue with dedicated instructions
				// - For everything else, branch on less or greater than zero
				case GREATER_THAN -> {
					if (isObject) {
						comparableCompare(mv);
						mv.visitJumpInsn(IFGT, label);
					} else if (intLike) {
						mv.visitJumpInsn(IF_ICMPGT, label);
					} else {
						mv.visitInsn(type.getOpcode(ISUB));
						mv.visitJumpInsn(IFGT, label);
					}
				}
				case GREATER_OR_EQUAL -> {
					if (isObject) {
						comparableCompare(mv);
						mv.visitJumpInsn(IFGE, label);
					} else if (intLike) {
						mv.visitJumpInsn(IF_ICMPGE, label);
					} else {
						mv.visitInsn(type.getOpcode(ISUB));
						mv.visitJumpInsn(IFGE, label);
					}
				}
				case LESS_THAN -> {
					if (isObject) {
						comparableCompare(mv);
						mv.visitJumpInsn(IFLT, label);
					} else if (intLike) {
						mv.visitJumpInsn(IF_ICMPLT, label);
					} else {
						mv.visitInsn(type.getOpcode(ISUB));
						mv.visitJumpInsn(IFLT, label);
					}
				}
				case LESS_OR_EQUAL -> {
					if (isObject) {
						comparableCompare(mv);
						mv.visitJumpInsn(IFLE, label);
					} else if (intLike) {
						mv.visitJumpInsn(IF_ICMPLE, label);
					} else {
						mv.visitInsn(type.getOpcode(ISUB));
						mv.visitJumpInsn(IFLE, label);
					}
				}
				case NULL -> mv.visitJumpInsn(IFNULL, label);
				case NOT_NULL -> mv.visitJumpInsn(IFNONNULL, label);
				case TRUE -> mv.visitJumpInsn(IFNE, label); // true == 1
				case FALSE -> mv.visitJumpInsn(IFEQ, label); // false == 0
				}				
			}));
		} else {			
			block.add(Bytecode.run(Type.VOID_TYPE, List.of(), mv -> {
				mv.visitJumpInsn(GOTO, label);
			}));
		}
	}
	
	private static final String EQUALS_DESCRIPTOR = Type.getMethodDescriptor(Type.BOOLEAN_TYPE,
			Type.getType(Object.class), Type.getType(Object.class));
	
	private void objectsEquals(MethodVisitor mv) {
		mv.visitMethodInsn(INVOKESTATIC, "java/util/Objects", "equals", EQUALS_DESCRIPTOR, false);
	}
	
	private static final String COMPARE_DESCRIPTOR = Type.getMethodDescriptor(Type.INT_TYPE, Type.getType(Object.class));
	
	private void comparableCompare(MethodVisitor mv) {
		mv.visitMethodInsn(INVOKEINTERFACE, "java/lang/Comparable", "compareTo", COMPARE_DESCRIPTOR, true);
	}

}