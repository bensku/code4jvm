package fi.benjami.code4jvm.statement;

import fi.benjami.code4jvm.Statement;
import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.Value;
import fi.benjami.code4jvm.block.Block;

import static org.objectweb.asm.Opcodes.*;

public class Throw implements Statement {

	private final Value value;
	
	private Throw(Value value) {
		this.value = value;
	}
	
	public static Statement value(Value value) {
		return new Throw(value);
	}

	@Override
	public void emitVoid(Block block) {
		block.add(Bytecode.run(Type.VOID, new Value[] {value}, ctx -> {
			ctx.asm().visitInsn(ATHROW);
		}, "throw"));
	}
	
	
}
