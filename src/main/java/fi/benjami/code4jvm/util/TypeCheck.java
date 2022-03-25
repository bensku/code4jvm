package fi.benjami.code4jvm.util;

import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.Value;

public class TypeCheck {

	public static void mustEqual(Type a, Type b) {
		if (!a.equals(b)) {
			throw new IllegalArgumentException("expected types " + a + " and " + b + " to be same");
		}
	}
	
	public static void mustEqual(Value a, Value b) {
		mustEqual(a.type(), b.type());
	}
	
	public static void mustBeSameSort(Type a, Type b) {
		if (a.isPrimitive() != b.isPrimitive()) {
			throw new IllegalArgumentException("expected types " + a + " and " + b + " to be same or both objects");
		}
	}
	
	public static void mustBeSameSort(Value a, Value b) {
		mustBeSameSort(a.type(), b.type());
	}
	
	public static void mustBeObject(Type type) {
		if (type.isPrimitive()) {
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
	
	public static void mustBeMethodOwner(Type type) {
		if (type.isPrimitive() || type.isArray()) {
			throw new IllegalArgumentException("type " + type + " cannot possible contain methods");
		}
	}

}
