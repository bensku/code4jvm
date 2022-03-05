package fi.benjami.code4jvm;

import fi.benjami.code4jvm.util.TypeCheck;

public class Condition {

	public enum Type {
		REF_EQUAL,
		REF_NOT_EQUAL,
		EQUAL,
		NOT_EQUAL,
		GREATER_THAN,
		LESS_THAN,
		GREATER_OR_EQUAL,
		LESS_OR_EQUAL,
		NULL,
		NOT_NULL,
		TRUE,
		FALSE
	}
	
	public static Condition refEqual(Value lhs, Value rhs) {
		TypeCheck.mustBeObject(lhs);
		TypeCheck.mustBeObject(rhs);
		return new Condition(Type.REF_EQUAL, Type.REF_NOT_EQUAL, lhs, rhs);
	}
	
	public static Condition equal(Value lhs, Value rhs) {
		TypeCheck.mustEqual(lhs, rhs);
		return new Condition(Type.EQUAL, Type.NOT_EQUAL, lhs, rhs);
	}
	
	public static Condition greaterThan(Value lhs, Value rhs) {
		return new Condition(Type.GREATER_THAN, Type.LESS_OR_EQUAL, lhs, rhs);
	}
	
	public static Condition lessThan(Value lhs, Value rhs) {
		return new Condition(Type.LESS_THAN, Type.GREATER_OR_EQUAL, lhs, rhs);
	}
	
	public static Condition greaterOrEqual(Value lhs, Value rhs) {
		return new Condition(Type.GREATER_OR_EQUAL, Type.LESS_THAN, lhs, rhs);
	}
	
	public static Condition lessOrEqual(Value lhs, Value rhs) {
		return new Condition(Type.LESS_OR_EQUAL, Type.GREATER_THAN, lhs, rhs);
	}
	
	public static Condition isNull(Value value) {
		TypeCheck.mustBeObject(value);
		return new Condition(Type.NULL, Type.NOT_NULL, value);
	}
	
	public static Condition isTrue(Value value) {
		TypeCheck.mustBe(value, org.objectweb.asm.Type.BOOLEAN_TYPE);
		return new Condition(Type.TRUE, Type.FALSE, value);
	}
	
	public static Condition isFalse(Value value) {
		TypeCheck.mustBe(value, org.objectweb.asm.Type.BOOLEAN_TYPE);
		return new Condition(Type.FALSE, Type.TRUE, value);
	}
		
	private final Type type;
	private final Type opposite;
	private final Value[] values;
	
	private Condition(Type type, Type opposite, Value... values) {
		this.type = type;
		this.opposite = opposite;
		this.values = values;
	}
	
	public Condition not() {
		return new Condition(opposite, type, values);
	}
	
	public Type type() {
		return type;
	}
	
	public Value[] values() {
		return values;
	}
}
