package fi.benjami.code4jvm.lua.ir;

import fi.benjami.code4jvm.Type;

public final class LuaLocalVar implements LuaVariable {
	
	public static final Type TYPE = Type.of(LuaLocalVar.class);
	
	public static final LuaLocalVar VARARGS = new LuaLocalVar("...");
	
	private final String name;
	private int mutationSites;
	private boolean upvalue;
	
	public LuaLocalVar(String name) {
		this.name = name;
	}
	
	public String name() {
		return name;
	}
	
	@Override
	public void markMutable() {
		mutationSites++;
	}
	
	/**
	 * Whether or not this local variable is ever assigned to after its initial
	 * assignment. This includes mutations by blocks that inherit it as upvalue
	 * (to be precise, Lua upvalues are essentially external local variables).
	 */
	public boolean mutable() {
		return mutationSites > 1;
	}
	
	public void markUpvalue() {
		upvalue = true;
	}
	
	/**
	 * Whether or not this local variable is an upvalue for some block.
	 */
	public boolean upvalue() {
		return upvalue;
	}

}
