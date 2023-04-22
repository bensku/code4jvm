package fi.benjami.code4jvm.lua.ir;

import java.util.List;

public record LuaBlock(
		List<IrNode> nodes
) implements IrNode {
	
}
