package fi.benjami.code4jvm.lua.ir;

public record TableField(
		IrNode table,
		String field
) implements LuaVariable {}
