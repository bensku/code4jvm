package fi.benjami.code4jvm.lua.runtime;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MutableCallSite;

import fi.benjami.code4jvm.lua.ir.LuaType;

/**
 * Metadata about a Lua call site.
 *
 */
public class LuaCallSite {
	
	/**
	 * The JVM call site. Null for constant call sites.
	 */
	public final MutableCallSite site;
	
	/**
	 * Method that relinks this call site, then proceeds directly to the new target.
	 */
	public final MethodHandle relink;
	
	/**
	 * Whether or not this site has unknown argument types. We track this
	 * because runtime type checks are pointless if all types are known.
	 */
	public final boolean hasUnknownTypes;
	
	/**
	 * Compile-time options for the call site.
	 */
	public final CallSiteOptions options;
	
	public LuaCallSite(MutableCallSite site, CallSiteOptions options) {
		this.site = site;
		this.relink = LuaLinker.UPDATE_SITE.bindTo(this);
		var unknownType = false;
		for (var type : options.types()) {
			if (type.equals(LuaType.UNKNOWN)) {
				unknownType = true;
			}
		}
		this.hasUnknownTypes = unknownType;
		this.options = options;
	}

	/**
	 * How many times this call site has been linked. This is done when it is
	 * first entered, and again if guards fail. The latter tends to occur when
	 * the target, its prototype or the types at the call site change.
	 */
	public int linkageCount;
	
	/**
	 * Whether or not this call site currently uses runtime types.
	 */
	public boolean usesRuntimeTypes;
	
}
