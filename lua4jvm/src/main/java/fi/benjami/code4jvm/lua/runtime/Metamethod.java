package fi.benjami.code4jvm.lua.runtime;

public enum Metamethod {
	ADD("__add"),
	SUBTRACT("__sub"),
	MULTIPLY("__mul"),
	DIVIDE("__div"),
	MODULO("__mod"),
	POWER("__pow");
	// TODO rest of metamethods when metatables are actually implemented
	
	private final String methodName;
	
	Metamethod(String name) {
		this.methodName = name;
	}
	
	public String methodName() {
		return methodName;
	}
}
