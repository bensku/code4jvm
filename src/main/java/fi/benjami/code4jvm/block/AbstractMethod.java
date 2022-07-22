package fi.benjami.code4jvm.block;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.Opcodes;

import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.flag.MethodFlag;

public final class AbstractMethod implements Method {
	
	private final Type returnType;
	private final String name;
	final int access;
	private final List<Type> argTypes;
	
	AbstractMethod(Type returnType, String name, MethodFlag[] flags, boolean isStatic, boolean isNative) {
		this.argTypes = new ArrayList<>();
		this.returnType = returnType;
		this.name = name;
		this.access = Method.getAccess(flags)
				| (isNative ? Opcodes.ACC_NATIVE : Opcodes.ACC_ABSTRACT)
				| (isStatic ? Opcodes.ACC_STATIC : 0);
	}
	
	public void arg(Type type) {
		argTypes.add(type);
	}
	
	public Type returnType() {
		return returnType;
	}
	
	public String name() {
		return name;
	}
	
	public List<Type> argumentTypes() {
		return argTypes;
	}
	
	public boolean isNative() {
		return (access & Opcodes.ACC_NATIVE) != 0;
	}
}
