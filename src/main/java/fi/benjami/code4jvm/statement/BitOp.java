package fi.benjami.code4jvm.statement;


import fi.benjami.code4jvm.Constant;
import fi.benjami.code4jvm.Expression;
import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.Value;
import fi.benjami.code4jvm.util.TypeCheck;
import fi.benjami.code4jvm.util.TypeUtils;

import static org.objectweb.asm.Opcodes.*;

public class BitOp {

	// TODO type checks
	
	public static Expression shiftLeft(Value lhs, Value rhs) {
		var type = lhs.type();
		return block -> {
			return block.add(Bytecode.run(type, new Value[] {lhs, rhs}, ctx -> {
				ctx.asm().visitInsn(type.getOpcode(ISHL, ctx));
			}, "shift left"));
		};
	}
	
	public static Expression shiftRightSigned(Value lhs, Value rhs) {
		var type = lhs.type();
		return block -> {
			return block.add(Bytecode.run(type, new Value[] {lhs, rhs}, ctx -> {
				ctx.asm().visitInsn(type.getOpcode(ISHR, ctx));
			}, "shift right (signed)"));
		};
	}
	
	public static Expression shiftRightUnsigned(Value lhs, Value rhs) {
		var type = lhs.type();
		return block -> {
			return block.add(Bytecode.run(type, new Value[] {lhs, rhs}, ctx -> {
				ctx.asm().visitInsn(type.getOpcode(IUSHR, ctx));
			}, "shift right (unsigned)"));
		};
	}
	
	public static Expression and(Value lhs, Value rhs) {
		TypeCheck.mustEqual(lhs, rhs);
		var type = lhs.type();
		return block -> {
			return block.add(Bytecode.run(type, new Value[] {lhs, rhs}, ctx -> {
				ctx.asm().visitInsn(type.getOpcode(IAND, ctx));
			}, "bitwise and"));
		};
	}
	
	public static Expression or(Value lhs, Value rhs) {
		TypeCheck.mustEqual(lhs, rhs);
		var type = lhs.type();
		return block -> {
			return block.add(Bytecode.run(type, new Value[] {lhs, rhs}, ctx -> {
				ctx.asm().visitInsn(type.getOpcode(IOR, ctx));
			}, "bitwise or"));
		};
	}
	
	public static Expression xor(Value lhs, Value rhs) {
		TypeCheck.mustEqual(lhs, rhs);
		var type = lhs.type();
		return block -> {
			return block.add(Bytecode.run(type, new Value[] {lhs, rhs}, ctx -> {
				ctx.asm().visitInsn(type.getOpcode(IXOR, ctx));
			}, "bitwise xor"));
		};
	}
	
	public static Expression not(Value arg) {
		Value mask;
		if (TypeUtils.isIntLike(arg.type())) {
			mask = Constant.of(~0);
		} else if (arg.type().equals(Type.LONG)) {
			mask = Constant.of(~0L);
		} else {
			throw new IllegalArgumentException("expected number type");
		}
		return xor(arg, mask);
	}
}
