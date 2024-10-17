package fi.benjami.code4jvm.lua.ir;

public sealed interface LuaVariable permits LuaLocalVar, TableField {

	void markMutable();
}
