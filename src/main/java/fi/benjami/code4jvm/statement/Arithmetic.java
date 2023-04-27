package fi.benjami.code4jvm.statement;

import fi.benjami.code4jvm.Expression;
import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.Value;
import fi.benjami.code4jvm.util.TypeCheck;

import static org.objectweb.asm.Opcodes.*;

/**
 * Simple arithmetic operations that are built-in to Java language and JVM
 * bytecode. For operations handled by {@link java.lang.Math}, call the methods
 * yourself with {@link Type#callStatic(Type, String, Value...)}.
 */
public class Arithmetic {
	
	public static Expression add(Value lhs, Value rhs) {
		TypeCheck.mustEqual(lhs, rhs);
		var type = lhs.type();
		return block -> {
			return block.add(Bytecode.run(type, new Value[] {lhs, rhs}, ctx -> {
				ctx.asm().visitInsn(type.getOpcode(IADD, ctx));
			}, "add"));
		};
	}
	
	public static Expression subtract(Value lhs, Value rhs) {
		TypeCheck.mustEqual(lhs, rhs);
		var type = lhs.type();
		return block -> {
			return block.add(Bytecode.run(type, new Value[] {lhs, rhs}, ctx -> {
				ctx.asm().visitInsn(type.getOpcode(ISUB, ctx));
			}, "subtract"));
		};
	}
	
	public static Expression multiply(Value lhs, Value rhs) {
		TypeCheck.mustEqual(lhs, rhs);
		var type = lhs.type();
		return block -> {
			return block.add(Bytecode.run(type, new Value[] {lhs, rhs}, ctx -> {
				ctx.asm().visitInsn(type.getOpcode(IMUL, ctx));
			}, "multiply"));
		};
	}
	
	public static Expression divide(Value lhs, Value rhs) {
		TypeCheck.mustEqual(lhs, rhs);
		var type = lhs.type();
		return block -> {
			return block.add(Bytecode.run(type, new Value[] {lhs, rhs}, ctx -> {
				ctx.asm().visitInsn(type.getOpcode(IDIV, ctx));
			}, "divide"));
		};
	}
	
	public static Expression remainder(Value lhs, Value rhs) {
		TypeCheck.mustEqual(lhs, rhs);
		var type = lhs.type();
		return block -> {
			return block.add(Bytecode.run(type, new Value[] {lhs, rhs}, ctx -> {
				ctx.asm().visitInsn(type.getOpcode(IREM, ctx));
			}, "remainder"));
		};
	}
}
