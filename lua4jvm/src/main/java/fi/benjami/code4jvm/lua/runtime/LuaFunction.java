package fi.benjami.code4jvm.lua.runtime;

import java.util.List;
import java.util.Map;

import fi.benjami.code4jvm.lua.ir.LuaBlock;
import fi.benjami.code4jvm.lua.ir.LuaLocalVar;

public record LuaFunction(
		/**
		 * The values captured by this function when it was defined.
		 */
		Map<LuaLocalVar, Object> upvalues,
		
		/**
		 * Arguments the function accepts.
		 */
		List<LuaLocalVar> args,
		
		/**
		 * Body of the function. This is used to compile specialized
		 * versions of it.
		 */
		LuaBlock body
		
		// TODO
) {

}
