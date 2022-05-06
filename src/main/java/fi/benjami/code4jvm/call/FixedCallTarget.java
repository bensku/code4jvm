package fi.benjami.code4jvm.call;

import org.objectweb.asm.Handle;

import fi.benjami.code4jvm.Expression;
import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.Value;

public abstract class FixedCallTarget extends CallTarget {
	
	private final Type owner;
	private final boolean ownerIsInterface;
	
	private final Type returnType;
	private final Type[] argTypes;
	
	FixedCallTarget(Type owner, boolean ownerIsInterface, Type returnType, String name, Type[] argTypes) {
		super(name);
		this.owner = owner;
		this.ownerIsInterface = ownerIsInterface;
		this.returnType = returnType;
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
	
	public Type[] argTypes() {
		return argTypes;
	}
	
	public abstract Handle toMethodHandle();
	
	public Expression call(Type returnType, Value... args) {
		return call(returnType, args);
	}
	
	public abstract Expression call(Value... args);

}
