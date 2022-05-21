package fi.benjami.code4jvm.flag;

import org.objectweb.asm.Opcodes;

public enum MethodFlag {

	DEPRECATED(Opcodes.ACC_DEPRECATED),
	SYNCHRONIZED(Opcodes.ACC_SYNCHRONIZED),
	SYNTHETIC(Opcodes.ACC_SYNTHETIC),
	BRIDGE(Opcodes.ACC_BRIDGE);
	
	private final int value;
	
	MethodFlag(int value) {
		this.value = value;
	}
	
	public int value() {
		return value;
	}
}
