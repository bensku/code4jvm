package fi.benjami.code4jvm;

import java.util.Optional;

import fi.benjami.code4jvm.block.Block;

public class Constant implements Value {
	
	public static Constant of(boolean value) {
		return new Constant(value, Type.BOOLEAN);
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
	
	private static final Type STRING = Type.of(String.class);
	
	public static Constant of(String value) {
		return new Constant(value, STRING);
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
