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
	
	protected LuaTable() {}
	
	protected void initializeMap(int size) {
		if (map == null) {			
			map = new HashMap<>(size);
		}
	}
	
	public Object get(Object key) {
		return map.get(key);
	}
	
	public void set(Object key, Object value) {
		map.put(key, value);
	}
	
	public int size() {
		return map.size();
	}
}
