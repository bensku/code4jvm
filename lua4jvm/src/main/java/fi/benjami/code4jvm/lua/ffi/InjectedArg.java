package fi.benjami.code4jvm.lua.ffi;

import fi.benjami.code4jvm.lua.linker.LuaCallSite;

public interface InjectedArg {

	Object get(LuaCallSite site);
}
