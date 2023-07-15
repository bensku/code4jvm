package fi.benjami.code4jvm.lua.ir;

public record LuaModule(
		LuaBlock root,
		LuaLocalVar env
) {}
