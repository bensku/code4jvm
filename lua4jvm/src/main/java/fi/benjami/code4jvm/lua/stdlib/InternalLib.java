package fi.benjami.code4jvm.lua.stdlib;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import fi.benjami.code4jvm.lua.ffi.JavaFunction;
import fi.benjami.code4jvm.lua.ffi.LuaBinder;
import fi.benjami.code4jvm.lua.ffi.LuaExport;
import fi.benjami.code4jvm.lua.runtime.LuaTable;

class InternalLib {
	
	public static final Map<String, JavaFunction> FUNCTIONS;
	
	static {
		var functions = new LuaBinder(MethodHandles.lookup()).bindFunctionsFrom(InternalLib.class);
		var map = new HashMap<String, JavaFunction>();
		for (var func : functions) {
			map.put(func.name(), func);
		}
		FUNCTIONS = Collections.unmodifiableMap(map);
	}

	private static final Object[] PAIRS_ARRAY = new Object[2];
	
	@LuaExport("intrinsicIterator")
	private static Object[] intrinsicIterator(LuaTable.Iterator iterator) {
		if (iterator.next()) {
			PAIRS_ARRAY[0] = iterator.key();
			PAIRS_ARRAY[1] = iterator.value();
		} else {
			PAIRS_ARRAY[0] = null; // Signal loop end
			PAIRS_ARRAY[1] = null; // ... and allow last value to be GC'd later
		}
		return PAIRS_ARRAY;
	}
	
	@LuaExport("tableIterator")
	private static Object[] tableIterator(LuaTable table, String prevKey) {
		return table.next(prevKey);
	}
}
