package fi.benjami.code4jvm.lua.ir;

import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.lua.compiler.CompilerPass;

public final class LuaLocalVar implements LuaVariable {
	
	public static final Type TYPE = Type.of(LuaLocalVar.class);
	
	public static final LuaLocalVar VARARGS = new LuaLocalVar("...");
	
	private final String name;
	private boolean upvalue;
	
	public LuaLocalVar(String name) {
		this.name = name;
	}
	
	public String name() {
		return name;
	}
	
	// The below mutations are safe, because they're done in IR compilation phase that is run once only
	
	public void markUpvalue() {
		assert CompilerPass.IR_GEN.active();
		upvalue = true;
	}
	
	/**
	 * Whether or not this local variable is an upvalue for some block.
	 */
	public boolean upvalue() {
		assert CompilerPass.IR_GEN.inactive();
		return upvalue;
	}

}
