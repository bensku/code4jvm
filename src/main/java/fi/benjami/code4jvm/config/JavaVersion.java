package fi.benjami.code4jvm.config;

import org.objectweb.asm.Opcodes;

/**
 * Java/JVM versions supported by code4jvm.
 *
 */
public enum JavaVersion {

	// FIXME although lambdas can be compiled for Java 7 bytecode, LambdaMetafactory is not available
	JAVA_7(Opcodes.V1_7),
	JAVA_8(Opcodes.V1_8),
	JAVA_9(Opcodes.V9),
	JAVA_10(Opcodes.V10),
	JAVA_11(Opcodes.V11),
	JAVA_12(Opcodes.V12),
	JAVA_13(Opcodes.V13),
	JAVA_14(Opcodes.V14),
	JAVA_15(Opcodes.V15),
	JAVA_16(Opcodes.V16),
	JAVA_17(Opcodes.V17),
	JAVA_18(Opcodes.V18),
	JAVA_19(Opcodes.V19),
	JAVA_21(Opcodes.V21);
	
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
