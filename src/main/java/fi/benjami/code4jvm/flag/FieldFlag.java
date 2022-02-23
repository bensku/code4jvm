package fi.benjami.code4jvm.flag;

import org.objectweb.asm.Opcodes;

public enum FieldFlag {

	SYNTHETIC(Opcodes.ACC_SYNTHETIC),
	DEPRECATED(Opcodes.ACC_DEPRECATED);
	
	private final int value;
	
	FieldFlag(int value) {
		this.value = value;
	}
	
	public int value() {
		return value;
	}
}
