package fi.benjami.code4jvm.statement;

import java.util.List;

import org.objectweb.asm.Type;

import fi.benjami.code4jvm.Statement;
import fi.benjami.code4jvm.Value;

import static org.objectweb.asm.Opcodes.*;

public class Return {

	public static Statement nothing() {
		return block -> {
			block.add(Bytecode.run(Type.VOID_TYPE, List.of(), mv -> {
				mv.visitInsn(RETURN);
			}));
		};
	}
	
	public static Statement value(Value value) {
		return block -> {
			block.add(Bytecode.run(Type.VOID_TYPE, List.of(value), mv -> {
				mv.visitInsn(value.type().getOpcode(IRETURN));
			}));
		};
	}
}
