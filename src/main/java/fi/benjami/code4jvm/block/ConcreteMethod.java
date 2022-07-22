package fi.benjami.code4jvm.block;

import fi.benjami.code4jvm.Type;

public sealed class ConcreteMethod extends Routine implements Method
		permits Method.Static, Method.Instance {
	
	private final String name;
	final int access;
	
	boolean framesComputed;
	
	ConcreteMethod(Block block, Type returnType, String name, int access) {
		super(block, returnType);
		this.name = name;
		this.access = access;
	}
	
	public String name() {
		return name;
	}
}
