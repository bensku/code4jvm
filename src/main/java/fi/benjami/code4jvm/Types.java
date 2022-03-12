package fi.benjami.code4jvm;

import java.util.Arrays;

import org.objectweb.asm.Type;

import fi.benjami.code4jvm.call.InitCallTarget;
import fi.benjami.code4jvm.util.TypeCheck;

public class Types {
	
	private Types() {}

	public static InitCallTarget getConstructor(Type type, Type... argTypes) {
		TypeCheck.mustBeObject(type);
		return new InitCallTarget(type, argTypes);
	}
	
	public static Expression newInstance(Type type, Value... args) {
		return getConstructor(type, Arrays.stream(args).map(Value::type).toArray(Type[]::new))
				.call(args);
	}
}
