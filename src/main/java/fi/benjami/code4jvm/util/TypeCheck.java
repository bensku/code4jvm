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
	
	public static void mustBeObject(Type type) {
		if (type.getSort() != Type.OBJECT) {
			throw new IllegalArgumentException("expected object type, got primitive " + type);
		}
	}
	
	public static void mustBeObject(Value value) {
		mustBeObject(value.type());
	}
	
	public static void mustBe(Type type, Type expected) {
		if (!type.equals(expected)) {
			throw new IllegalArgumentException("expected type " + expected + ", got " + type);
		}
	}
	
	public static void mustBe(Value value, Type expected) {
		mustBe(value.type(), expected);
	}

}
