package fi.benjami.code4jvm.lua.compiler;

import java.util.HashSet;
import java.util.Set;

public class CompiledShape {
	
	/**
	 * Keys that are present in this compiled shape.
	 * This MAY be different from shape's known keys, if more are added
	 * after it has been compiled.
	 * 
	 * <p>For example, if a table that was passed as an argument can't
	 * have its shape modified. Still, if keys are added to it, types of
	 * those entries will be known within that function.
	 */
	private final Set<String> includedKeys;
	
	/**
	 * True if this shape is known to have unknown keys that need to be stored
	 * in the map.
	 */
	private boolean initializeMap;

	/**
	 * The class this shape was compiled to.
	 */
	private Class<?> backingClass;
	
	public CompiledShape() {
		this.includedKeys = new HashSet<>();
	}
	
	public Set<String> includedKeys() {
		return includedKeys;
	}
	
	public boolean shouldInitializeMap() {
		return initializeMap;
	}
	
	public Class<?> backingClass() {
		if (backingClass == null) {
			backingClass = ShapeGenerator.compile(this);
		}
		return backingClass;
	}
	
	public void addKnownKey(String key) {
		if (backingClass == null) {			
			includedKeys.add(key);
		}
	}
	
	public void addUnknownKey() {
		if (backingClass == null) {
			initializeMap = true;
		}
	}
	
}
