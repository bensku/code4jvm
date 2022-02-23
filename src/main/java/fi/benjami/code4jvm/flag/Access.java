package fi.benjami.code4jvm.flag;

import org.objectweb.asm.Opcodes;

public enum Access {

	PRIVATE(Opcodes.ACC_PRIVATE),
	PACKAGE_PRIVATE(0), // Default in class-file format
	PROTECTED(Opcodes.ACC_PROTECTED),
	PUBLIC(Opcodes.ACC_PUBLIC);
	
	private final int value;
	
	Access(int value) {
		this.value = value;
	}
	
	public int value() {
		return value;
	}
}
