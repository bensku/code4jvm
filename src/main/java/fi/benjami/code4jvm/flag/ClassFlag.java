package fi.benjami.code4jvm.flag;

import org.objectweb.asm.Opcodes;

public enum ClassFlag {

	DEPRECATED(Opcodes.ACC_DEPRECATED);
	
	private final int value;
	
	ClassFlag(int value) {
		this.value = value;
	}
	
	public int value() {
		return value;
	}
}