package fi.benjami.code4jvm.lua.runtime;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class TableAccess {

	public static final DynamicTarget GET_TARGET, SET_TARGET;
	
	static {
		var lookup = MethodHandles.lookup();
		MethodHandle getRaw;
		try {
			getRaw = MethodHandles.dropArguments(lookup.findVirtual(LuaTable.class, "getRaw",
					MethodType.methodType(Object.class, Object.class)), 0, Object.class);
			var setRaw = MethodHandles.dropArguments(lookup.findVirtual(LuaTable.class, "setRaw",
					MethodType.methodType(void.class, Object.class, Object.class)), 0, Object.class);
			var get = MethodHandles.dropArguments(lookup.findVirtual(LuaTable.class, "get",
					MethodType.methodType(Object.class, Object.class)), 0, Object.class);
			var set = MethodHandles.dropArguments(lookup.findVirtual(LuaTable.class, "set",
					MethodType.methodType(void.class, Object.class, Object.class)), 0, Object.class);
			var checkChanges = lookup.findStatic(TableAccess.class, "checkChanges",
					MethodType.methodType(boolean.class, LuaCallSite.class, Object.class, Object.class));
			
			// TODO this is probably SLOWER than just calling LuaTable#get!
			// (but this allows for interesting optimizations!)
			// TODO SET_TARGET is unused, fix that
			GET_TARGET = new DynamicTarget((site, args) -> {
				if (args[0] instanceof LuaTable table && table.getMetatable() == null) {
					site.directTableAccess = true;
					return getRaw;
				}
				site.directTableAccess = false;
				return get;
			}, checkChanges);
			SET_TARGET = new DynamicTarget((site, args) -> {
				if (args[0] instanceof LuaTable table && table.getMetatable() == null) {
					site.directTableAccess = true;
					return setRaw;
				}
				site.directTableAccess = false;
				return set;
			}, checkChanges);
		} catch (NoSuchMethodException | IllegalAccessException e) {
			throw new AssertionError(e);
		}
	}
	
	@SuppressWarnings("unused") // MethodHandle
	private static boolean checkChanges(LuaCallSite site, Object callable, Object tbl) {
		// TODO check if callable == GET/SET_TARGET ?
		// If we were using direct table access, but can't now (or the other way around) -> relink!
		var simpleTable = tbl instanceof LuaTable table && table.getMetatable() == null;
		return simpleTable == site.directTableAccess;
	}
}
