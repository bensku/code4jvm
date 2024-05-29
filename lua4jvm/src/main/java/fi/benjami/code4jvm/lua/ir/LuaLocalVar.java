package fi.benjami.code4jvm.lua.ir;

import fi.benjami.code4jvm.Type;

public record LuaLocalVar(
		String name
) implements LuaVariable {
	
	public static final Type TYPE = Type.of(LuaLocalVar.class);
	
	public static final LuaLocalVar VARARGS = new LuaLocalVar("...");

}
