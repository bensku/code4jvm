package fi.benjami.code4jvm.lua.ir;

public record TableField(
		IrNode table,
		IrNode field
) implements LuaVariable {

	@Override
	public void markMutable() {
		// Do nothing, table fields are always mutable
	}
}
