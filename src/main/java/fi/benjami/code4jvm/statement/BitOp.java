package fi.benjami.code4jvm.statement;


import fi.benjami.code4jvm.Expression;
import fi.benjami.code4jvm.Value;
import fi.benjami.code4jvm.util.TypeCheck;

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
}
