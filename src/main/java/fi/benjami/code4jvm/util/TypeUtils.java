package fi.benjami.code4jvm.util;

import org.objectweb.asm.Type;

public class TypeUtils {
	public static boolean isIntLike(Type type) {
		return type.equals(Type.BYTE_TYPE) || type.equals(Type.SHORT_TYPE) || type.equals(Type.CHAR_TYPE) || type.equals(Type.INT_TYPE);
	}
}
