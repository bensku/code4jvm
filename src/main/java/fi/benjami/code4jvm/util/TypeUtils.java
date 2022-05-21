package fi.benjami.code4jvm.util;

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
	
}
