package fi.benjami.code4jvm.lua.runtime;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
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
	private final boolean hasUnknownTypes;
	
	public LuaCallSite(MutableCallSite site, LuaType[] types) {
		this.site = site;
		this.relink = MethodHandles.insertArguments(LuaLinker.UPDATE_SITE, 0, this, types);
		var unknownType = false;
		for (var type : types) {
			if (type.equals(LuaType.UNKNOWN)) {
				unknownType = true;
			}
		}
		this.hasUnknownTypes = unknownType;
	}

	/**
	 * How many times this call site has been re-linked. Linkage is done when
	 * the target or prototype of target have changed.
	 */
	public int linkageCount;
	
	/**
	 * How many times the argument types of this call site have changed.
	 */
	public int typeChangeCount;
	
	/**
	 * Prototype of the current call target.
	 */
	public Object currentPrototype;
	
	/**
	 * The current callable.
	 */
	public Object currentCallable;
	
	/**
	 * Checks if this site should consider runtime types of the arguments.
	 * @return Whether or not to enable runtime type checks.
	 */
	public boolean shouldUseRuntimeTypes() {
		// TODO better heuristics; runtime type checks make calls slower, but improve target code generation
		return hasUnknownTypes && typeChangeCount < 3;
	}
	
	/**
	 * Checks whether or not this site should consider identity of the target,
	 * or merely prototype of it. This is only applicable to Lua functions.
	 * @return True for identity checks, false otherwise.
	 */
	public boolean shouldCheckTarget() {
		return linkageCount < 5; // TODO configurable
	}
	
}
