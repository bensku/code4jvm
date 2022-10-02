package fi.benjami.code4jvm;

import java.util.Optional;

public class Constant implements Value {
	
	private static final Constant TRUE = new Constant(true, Type.BOOLEAN), FALSE = new Constant(false, Type.BOOLEAN);
	
	public static Constant of(boolean value) {
		return value ? TRUE : FALSE;
	}
	
	public static Constant of(byte value) {
		return new Constant(value, Type.BYTE);
	}
	
	public static Constant of(short value) {
		return new Constant(value, Type.SHORT);
	}
	
	public static Constant of(char value) {
		return new Constant(value, Type.CHAR);
	}
	
	public static Constant of(int value) {
		return new Constant(value, Type.INT);
	}
	
	public static Constant of(float value) {
		return new Constant(value, Type.FLOAT);
	}
	
	public static Constant of(long value) {
		return new Constant(value, Type.LONG);
	}
	
	public static Constant of(double value) {
		return new Constant(value, Type.DOUBLE);
	}
	
	public static Constant of(String value) {
		return new Constant(value, Type.STRING);
	}
	
	private static final Type CLASS = Type.of(Class.class);
	
	public static Constant of(Type value) {
		return new Constant(value, CLASS);
	}
	
	// TODO class reference, method handle, constant dynamic
	
	private final Object value;
	private final Type type;
	
	private Constant(Object value, Type type) {
		this.value = value;
		this.type = type;
	}
	
	public Object value() {
		return value;
	}
	
	public Object asmValue() {
		// TODO change when dynamic constants are supported
		if (value instanceof Type type) {
			// Convert from our Type to ASM Type
			return org.objectweb.asm.Type.getType(type.descriptor());
		} else {
			return value;
		}
	}
	
	@Override
	public Type type() {
		return type;
	}

	@Override
	public Optional<String> name() {
		return Optional.empty();
	}

	@Override
	public Value original() {
		return this;
	}
	
	@Override
	public String toString() {
		return "const{" + type + " " + value + "}";
	}

}
