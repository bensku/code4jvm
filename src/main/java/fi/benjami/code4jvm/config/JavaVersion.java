package fi.benjami.code4jvm.config;

import org.objectweb.asm.Opcodes;

/**
 * Java/JVM versions supported by code4jvm.
 *
 */
public enum JavaVersion {

	JAVA_17(Opcodes.V17);
	
	private final int opcode;
	
	JavaVersion(int opcode) {
		this.opcode = opcode;
	}
	
	public boolean isAtLeast(JavaVersion version) {
		return opcode >= version.opcode;
	}
	
	public int opcode() {
		return opcode;
	}
}
