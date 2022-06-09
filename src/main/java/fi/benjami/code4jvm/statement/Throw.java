package fi.benjami.code4jvm.statement;

import fi.benjami.code4jvm.Statement;
import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.Value;

import static org.objectweb.asm.Opcodes.*;

public class Throw {

	public static Statement value(Value value) {
		return block -> {
			block.add(Bytecode.run(Type.VOID, new Value[] {value}, ctx -> {
				ctx.asm().visitInsn(ATHROW);
			}));
		};
	}
}
