package fi.benjami.code4jvm.util;

import static org.objectweb.asm.Opcodes.*;

import fi.benjami.code4jvm.Type;

public class TypeUtils {
	
	public static boolean isIntLike(Type type) {
		return type.equals(Type.BYTE) || type.equals(Type.SHORT) || type.equals(Type.CHAR) || type.equals(Type.INT);
	}
	
	public static String methodDescriptor(Type returnType, Type... argTypes) {
		var sb = new StringBuilder("(");
		for (var type : argTypes) {
			sb.append(type.descriptor());
		}
		sb.append(")");
		sb.append(returnType.descriptor());
		return sb.toString();
	}
	
	public static String instanceMethodDescriptor(Type returnType, Type... argTypes) {
		var sb = new StringBuilder("(");
		for (int i = 1; i < argTypes.length; i++) {
			sb.append(argTypes[i].descriptor());
		}
		sb.append(")");
		sb.append(returnType.descriptor());
		return sb.toString();
	}
	
	public static int getNewarrayOperand(Type type) {
		if (type.equals(Type.BOOLEAN)) {
			return T_BOOLEAN;
		} else if (type.equals(Type.BYTE)) {
			return T_BYTE;
		} else if (type.equals(Type.SHORT)) {
			return T_SHORT;
		} else if (type.equals(Type.CHAR)) {
			return T_CHAR;
		} else if (type.equals(Type.INT)) {
			return T_INT;
		} else if (type.equals(Type.LONG)) {
			return T_LONG;
		} else if (type.equals(Type.FLOAT)) {
			return T_FLOAT;
		} else if (type.equals(Type.DOUBLE)) {
			return T_DOUBLE;
		} else {
			throw new AssertionError(type);
		}
	}
	
	public static int slotCount(Type type) {
		if (type.equals(Type.VOID)) {
			return 0;
		} else if (type.equals(Type.LONG) || type.equals(Type.DOUBLE)
				|| type.equals(Type.METHOD_RETURN_TYPE)) {
			// We don't actually know what METHOD_RETURN_TYPE is, assume long/double just in case
			return 2;
		} else {
			return 1;
		}
	}
	
}
