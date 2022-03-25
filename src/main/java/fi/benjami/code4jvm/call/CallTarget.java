package fi.benjami.code4jvm.call;

import fi.benjami.code4jvm.Expression;
import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.Value;

public abstract class CallTarget {
	
	private final Type owner;
	private final boolean ownerIsInterface;
	
	private final Type returnType;
	private final String name;
	private final Type[] argTypes;
	
	CallTarget(Type owner, boolean ownerIsInterface, Type returnType, String name, Type[] argTypes) {
		this.owner = owner;
		this.ownerIsInterface = ownerIsInterface;
		this.returnType = returnType;
		this.name = name;
		this.argTypes = argTypes;
	}
	
	public Type owner() {
		return owner;
	}
	
	public boolean ownerIsInterface() {
		return ownerIsInterface;
	}
	
	public Type returnType() {
		return returnType;
	}
	
	public String name() {
		return name;
	}
	
	public Type[] argTypes() {
		return argTypes;
	}
	
	public abstract Expression call(Value... args);
	
}
