package fi.benjami.code4jvm.statement;

import org.objectweb.asm.Opcodes;

import fi.benjami.code4jvm.Expression;
import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.Value;

public class Instanceof {

	public static Expression isInstance(Value value, Type type) {
		return Bytecode.run(Type.BOOLEAN, new Value[] {value}, ctx -> {
			ctx.asm().visitTypeInsn(Opcodes.INSTANCEOF, type.descriptor());
		}, "instanceof");
	}
}
