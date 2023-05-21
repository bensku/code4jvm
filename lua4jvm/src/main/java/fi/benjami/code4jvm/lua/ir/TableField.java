package fi.benjami.code4jvm.lua.ir;

public record TableField(
		IrNode table,
		IrNode field
) implements LuaVariable {}
