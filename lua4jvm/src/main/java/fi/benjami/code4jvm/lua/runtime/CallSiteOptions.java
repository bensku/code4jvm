package fi.benjami.code4jvm.lua.runtime;

import fi.benjami.code4jvm.lua.ir.LuaType;

public record CallSiteOptions(
		/**
		 * Argument types of the call site. These are the compile-time types,
		 * which may include UNKNOWNs.
		 */
		LuaType[] types,
		
		/**
		 * True if the call site wants to receive a multival, false
		 * otherwise. If this is false and the target would return
		 * a multival, the target should instead return the first value of
		 * the multival. If this is true, the target may return multival
		 * or a single value and the caller must account for that.
		 */
		boolean spreadResults,
		
		/**
		 * True if the call site wants to use another function call or '...'
		 * as the its last argument. This forces the linker to inspect the
		 * arguments and (attempt to) spread it over arguments.
		 */
		boolean spreadArguments
) {

}
