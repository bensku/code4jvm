package fi.benjami.code4jvm.flag;

import org.objectweb.asm.Opcodes;

public enum ClassFlag {

	ABSTRACT(Opcodes.ACC_ABSTRACT),
	INTERFACE(Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT),
	DEPRECATED(Opcodes.ACC_DEPRECATED),
	FINAL(Opcodes.ACC_FINAL),
	ENUM(Opcodes.ACC_ENUM);
	
	private final int value;
	
	ClassFlag(int value) {
		this.value = value;
	}
	
	public int value() {
		return value;
	}
}