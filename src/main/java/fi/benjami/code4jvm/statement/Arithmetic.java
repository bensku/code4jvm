package fi.benjami.code4jvm.statement;

import java.util.List;

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
			return block.add(Bytecode.run(type, List.of(lhs, rhs), mv -> {
				mv.visitInsn(type.getOpcode(IADD));
			})).value();
		};
	}
	
	public static Expression subtract(Value lhs, Value rhs) {
		TypeCheck.mustEqual(lhs, rhs);
		var type = lhs.type();
		return block -> {
			return block.add(Bytecode.run(type, List.of(lhs, rhs), mv -> {
				mv.visitInsn(type.getOpcode(ISUB));
			})).value();
		};
	}
	
	public static Expression multiply(Value lhs, Value rhs) {
		TypeCheck.mustEqual(lhs, rhs);
		var type = lhs.type();
		return block -> {
			return block.add(Bytecode.run(type, List.of(lhs, rhs), mv -> {
				mv.visitInsn(type.getOpcode(IMUL));
			})).value();
		};
	}
	
	public static Expression divide(Value lhs, Value rhs) {
		TypeCheck.mustEqual(lhs, rhs);
		var type = lhs.type();
		return block -> {
			return block.add(Bytecode.run(type, List.of(lhs, rhs), mv -> {
				mv.visitInsn(type.getOpcode(IDIV));
			})).value();
		};
	}
	
	public static Expression remainder(Value lhs, Value rhs) {
		TypeCheck.mustEqual(lhs, rhs);
		var type = lhs.type();
		return block -> {
			return block.add(Bytecode.run(type, List.of(lhs, rhs), mv -> {
				mv.visitInsn(type.getOpcode(ISUB));
			})).value();
		};
	}
}
