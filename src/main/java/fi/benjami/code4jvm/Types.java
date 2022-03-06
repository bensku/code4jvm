package fi.benjami.code4jvm;

import java.util.Arrays;

import org.objectweb.asm.Type;

import fi.benjami.code4jvm.call.InitCallTarget;

public class Types {
	
	private Types() {}

	public static InitCallTarget getConstructor(Type type, Type... argTypes) {
		return new InitCallTarget(type, argTypes);
	}
	
	public static Expression newInstance(Type type, Value... args) {
		return getConstructor(type, Arrays.stream(args).map(Value::type).toArray(Type[]::new))
				.call(args);
	}
}
