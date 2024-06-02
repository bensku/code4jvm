package fi.benjami.code4jvm.lua.linker;

/**
 * Dynamic targets are callable like Lua functions. However, when first
 * called, they can decide where the method calls lead to. They can also
 * control when to trigger re-linkage.
 *
 */
public interface DynamicTarget {
	
	LuaCallTarget resolve(LuaCallSite meta, Object[] args);
}
