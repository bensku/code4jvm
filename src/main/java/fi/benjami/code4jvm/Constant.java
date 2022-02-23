package fi.benjami.code4jvm;

import java.util.Optional;

import org.objectweb.asm.Type;

public class Constant implements Value {
	
	public static Constant of(int value) {
		return new Constant(value, Type.INT_TYPE);
	}
	
	public static Constant of(float value) {
		return new Constant(value, Type.FLOAT_TYPE);
	}
	
	public static Constant of(long value) {
		return new Constant(value, Type.LONG_TYPE);
	}
	
	public static Constant of(double value) {
		return new Constant(value, Type.DOUBLE_TYPE);
	}
	
	private static final Type STRING_TYPE = Type.getType(String.class);
	
	public static Constant of(String value) {
		return new Constant(value, STRING_TYPE);
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
	
	@Override
	public Type type() {
		return type;
	}

	@Override
	public Optional<Block> parentBlock() {
		return Optional.empty();
	}

	@Override
	public Optional<String> name() {
		return Optional.empty();
	}

}
