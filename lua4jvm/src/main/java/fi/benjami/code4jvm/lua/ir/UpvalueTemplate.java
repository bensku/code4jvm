package fi.benjami.code4jvm.lua.ir;

public record UpvalueTemplate(
		LuaLocalVar variable,
		LuaType type
) {}
