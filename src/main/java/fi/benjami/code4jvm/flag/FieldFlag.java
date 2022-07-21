package fi.benjami.code4jvm.flag;

import org.objectweb.asm.Opcodes;

public enum FieldFlag {

	FINAL(Opcodes.ACC_FINAL),
	VOLATILE(Opcodes.ACC_VOLATILE),
	TRANSIENT(Opcodes.ACC_TRANSIENT),
	SYNTHETIC(Opcodes.ACC_SYNTHETIC),
	ENUM(Opcodes.ACC_ENUM);
	
	private final int value;
	
	FieldFlag(int value) {
		this.value = value;
	}
	
	public int value() {
		return value;
	}
}
