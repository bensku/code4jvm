package fi.benjami.code4jvm.lua.compiler;

import java.util.HashMap;
import java.util.Map;

import fi.benjami.code4jvm.lua.ir.LuaType;

public class ShapeTypes {
	
	private final Map<String, LuaType> knownTypes;
	
	private boolean noTypeDiscovery;
	
	public ShapeTypes() {
		this.knownTypes = new HashMap<>();
	}

	/**
	 * Record an unknown write. This flushes types, but they can be
	 * re-discovered when writes are done.
	 */
	public void unknownWrite() {
		knownTypes.clear();
	}
	
	/**
	 * Record a metatable change. This flushes types AND makes disables
	 * discovering them in future.
	 */
	public void metatableChange() {
		knownTypes.clear();
		noTypeDiscovery = true;
	}
	
	/**
	 * Mark this shape as having escaped mutation analysis. This means that
	 * from now on, we have no idea about its content or (possible) metatable.
	 * 
	 * <p>Shapes commonly escape when they are passed as arguments to unknown
	 * functions.
	 */
	public void escapedAnalysis() {
		metatableChange();
	}
	
	public void recordType(String key, LuaType type) {
		if (!noTypeDiscovery) {
			knownTypes.put(key, type);
		}
	}
	
	public LuaType getType(String key) {
		return knownTypes.getOrDefault(key, LuaType.UNKNOWN);
	}
}
