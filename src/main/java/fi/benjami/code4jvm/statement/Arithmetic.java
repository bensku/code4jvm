package fi.benjami.code4jvm.statement;

import java.util.List;

import fi.benjami.code4jvm.Expression;
import fi.benjami.code4jvm.Value;
import fi.benjami.code4jvm.util.TypeCheck;

import static org.objectweb.asm.Opcodes.*;

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
}
