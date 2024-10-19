package fi.benjami.code4jvm.lua.debug;

import fi.benjami.code4jvm.lua.linker.LuaCallSite;

public class LinkerTrace {

	public LuaCallSite metadata;
	
	public Object callable;
	
	public Object currentPrototype;
	
	public int stableTargets;
}
