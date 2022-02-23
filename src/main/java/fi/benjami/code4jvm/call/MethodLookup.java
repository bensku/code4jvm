package fi.benjami.code4jvm.call;

import java.util.Arrays;

import org.objectweb.asm.Type;

import fi.benjami.code4jvm.Expression;
import fi.benjami.code4jvm.Value;

public interface MethodLookup<T extends CallTarget> {
	
	static MethodLookup<InstanceCallTarget> ofInstance(Type owner, Value instance, boolean ownerIsInterface, Linkage linkage)  {
		return (returnType, name, argTypes) -> new InstanceCallTarget(owner, instance, ownerIsInterface, linkage, returnType, name, argTypes);
	}
	
	static MethodLookup<StaticCallTarget> ofStatic(Type owner, boolean ownerIsInterface) {
		return (returnType, name, argTypes) -> new StaticCallTarget(owner, ownerIsInterface, returnType, name, argTypes);
	}
	
	T findMethod(Type returnType, String name, Type... argTypes);
	
	default Expression call(Type returnType, String name, Value... args) {
		var argTypes = Arrays.stream(args).map(Value::type).toArray(Type[]::new);
		return findMethod(returnType, name, argTypes).call(args);
	}
}
