package fi.benjami.code4jvm.lua.ir;

import fi.benjami.code4jvm.Value;
import fi.benjami.code4jvm.block.Block;
import fi.benjami.code4jvm.lua.compiler.LuaContext;

public interface IrNode {

	Value emit(LuaContext ctx, Block block);
	
	LuaType outputType(LuaContext ctx);
	
	default boolean hasReturn() {
		return false;
	}
}
