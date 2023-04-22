package fi.benjami.code4jvm.lua.runtime;

import java.util.List;

import fi.benjami.code4jvm.lua.ir.LuaBlock;
import fi.benjami.code4jvm.lua.ir.LuaValue;

public record LuaFunction(
		/**
		 * Values that the function captured when it was defined.
		 */
		List<Object> upvalues,
		
		/**
		 * Arguments the function accepts.
		 */
		List<LuaValue> args,
		
		/**
		 * Body of the function. This is used to compile specialized
		 * versions of it.
		 */
		LuaBlock body
		
		// TODO
) {

}
