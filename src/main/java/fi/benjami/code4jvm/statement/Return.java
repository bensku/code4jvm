package fi.benjami.code4jvm.statement;

import fi.benjami.code4jvm.Statement;
import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.Value;

import static org.objectweb.asm.Opcodes.*;

public class Return {

	public static Statement nothing() {
		return block -> {
			block.add(Bytecode.run(Type.VOID, new Value[0], mv -> {
				mv.visitInsn(RETURN);
			}));
		};
	}
	
	public static Statement value(Value value) {
		if (value.type().equals(Type.VOID)) {
			return nothing();
		}
		return block -> {
			block.add(Bytecode.run(Type.VOID, new Value[] {value}, mv -> {
				mv.visitInsn(value.type().getOpcode(IRETURN));
			}));
		};
	}
}
