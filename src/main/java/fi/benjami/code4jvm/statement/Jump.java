package fi.benjami.code4jvm.statement;

import org.objectweb.asm.MethodVisitor;

import fi.benjami.code4jvm.Condition;
import fi.benjami.code4jvm.Statement;
import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.Value;
import fi.benjami.code4jvm.block.Block;
import fi.benjami.code4jvm.util.TypeUtils;

import static org.objectweb.asm.Opcodes.*;

import java.util.Optional;

public class Jump implements Statement {
	
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
	
	private final Block targetBlock;
	private final Target target;
	private final Condition condition;
		
	private Jump(Block block, Target target, Condition condition) {
		this.targetBlock = block;
		this.target = target;
		this.condition = condition;
	}
	
	public Block block() {
		return targetBlock;
	}
	
	public Target target() {
		return target;
	}
	
	public Optional<Condition> condition() {
		return Optional.ofNullable(condition);
	}

	@Override
	public void emitVoid(Block block) {
		var label = block.add(new Block.Edge(targetBlock, target, condition != null, new Type[0]));
		if (condition != null) {
			var type = condition.values()[0].type();
			var isObject = type.isObject();
			var intLike = TypeUtils.isIntLike(type);
			block.add(Bytecode.run(Type.VOID, condition.values(), ctx -> {
				var mv = ctx.asm();
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
						mv.visitJumpInsn(IFNE, label);
					} else if (intLike) {
						mv.visitJumpInsn(IF_ICMPEQ, label);
					} else {
						mv.visitInsn(primitiveCompare(type, true));
						mv.visitJumpInsn(IFEQ, label);
					}
				}
				case NOT_EQUAL -> {
					if (isObject) {
						objectsEquals(mv);
						mv.visitJumpInsn(IFEQ, label);
					} else if (intLike) {
						mv.visitJumpInsn(IF_ICMPNE, label);
					} else {
						mv.visitInsn(primitiveCompare(type, true));
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
						mv.visitInsn(primitiveCompare(type, true));
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
						mv.visitInsn(primitiveCompare(type, true));
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
						mv.visitInsn(primitiveCompare(type, false));
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
						mv.visitInsn(primitiveCompare(type, false));
						mv.visitJumpInsn(IFLE, label);
					}
				}
				case NULL -> mv.visitJumpInsn(IFNULL, label);
				case NOT_NULL -> mv.visitJumpInsn(IFNONNULL, label);
				case TRUE -> mv.visitJumpInsn(IFNE, label); // true == 1
				case FALSE -> mv.visitJumpInsn(IFEQ, label); // false == 0
				}				
			}, "conditional jump"));
		} else {			
			block.add(Bytecode.run(Type.VOID, new Value[0], ctx -> {
				ctx.asm().visitJumpInsn(GOTO, label);
			}, "unconditional jump"));
		}
		// Note: debug names are intentionally bare-bones
		// When printing blocks, JumpNodes get special handling so that
		// label information from frames can be included in them
	}
	
	private int primitiveCompare(Type type, boolean negativeNan) {
		if (type.equals(Type.LONG)) {
			return LCMP;
		} else if (type.equals(Type.FLOAT)) {
			return negativeNan ? FCMPL : FCMPG;
		} else if (type.equals(Type.DOUBLE)) {
			return negativeNan ? DCMPL : DCMPG;
		}
		throw new AssertionError();
	}
	
	private static final String EQUALS_DESCRIPTOR = TypeUtils.methodDescriptor(Type.BOOLEAN, Type.OBJECT, Type.OBJECT);
	
	private void objectsEquals(MethodVisitor mv) {
		mv.visitMethodInsn(INVOKESTATIC, "java/util/Objects", "equals", EQUALS_DESCRIPTOR, false);
	}
	
	private static final String COMPARE_DESCRIPTOR = TypeUtils.methodDescriptor(Type.INT, Type.of(Object.class));
	
	private void comparableCompare(MethodVisitor mv) {
		mv.visitMethodInsn(INVOKEINTERFACE, "java/lang/Comparable", "compareTo", COMPARE_DESCRIPTOR, true);
	}

}
