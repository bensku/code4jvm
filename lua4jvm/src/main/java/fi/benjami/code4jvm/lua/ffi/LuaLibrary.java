package fi.benjami.code4jvm.lua.ffi;

import fi.benjami.code4jvm.lua.LuaVm;
import fi.benjami.code4jvm.lua.VmOptions;

/**
 * A library of functions (or other constructs) installable to a Lua VM.
 * @see VmOptions
 *
 */
public interface LuaLibrary {

	void install(LuaVm vm);
}
