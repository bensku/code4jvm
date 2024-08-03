package fi.benjami.code4jvm.lua.ir;

public record LuaModule(
		String name,
		LuaBlock root,
		LuaLocalVar env
) {}
