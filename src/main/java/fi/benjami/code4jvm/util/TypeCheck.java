package fi.benjami.code4jvm.util;

import org.objectweb.asm.Type;

import fi.benjami.code4jvm.Value;

public class TypeCheck {

	public static void mustEqual(Type a, Type b) {
		if (!a.equals(b)) {
			throw new IllegalArgumentException("expected types " + a + " and " + b + " to be same; did you forget to cast?");
		}
	}
	
	public static void mustEqual(Value a, Value b) {
		mustEqual(a.type(), b.type());
	}
}
