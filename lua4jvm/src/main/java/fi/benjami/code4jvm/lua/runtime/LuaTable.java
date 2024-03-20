package fi.benjami.code4jvm.lua.runtime;

import java.util.HashMap;
import java.util.Map;

import fi.benjami.code4jvm.Type;

public class LuaTable {
	
	public static LuaTable newTable(int initialSize) {
		var table = new LuaTable();
		table.initializeMap(initialSize);
		return table;
	}
	
	public static final Type TYPE = Type.of(LuaTable.class);

	private Map<Object, Object> map;
	private Object metatable;
	
	protected LuaTable() {}
	
	protected void initializeMap(int size) {
		if (map == null) {			
			map = new HashMap<>(size);
		}
	}
	
	public Object getRaw(Object key) {
		return map.get(key);
	}
	
	public void setRaw(Object key, Object value) {
		map.put(key, value);
	}
	
	// TODO metatable __index and __newindex support
	
	public Object get(Object key) {
		return getRaw(key);
	}
	
	public void set(Object key, Object value) {
		setRaw(key, value);
	}
	
	public Object getMetatable() {
		return metatable;
	}
	
	public void setMetatable(Object metatable) {
		this.metatable = metatable;
	}
	
	public int size() {
		return map.size();
	}
}
