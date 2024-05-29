package fi.benjami.code4jvm.lua.runtime;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import fi.benjami.code4jvm.lua.ir.LuaType;

/**
 * Linker support for table access.
 *
 */
public class TableAccess {
	
	private static final int MAX_LINKAGE_COUNT = 3;

	private static final MethodHandle CHECK_TABLE_SHAPE, CHECK_TABLE_AND_META_SHAPES;
	private static final MethodHandle GET_ARRAY, SET_ARRAY, GET_AT, SET_AT, GET_RAW, SET_RAW, GET, SET;
	
	public static final DynamicTarget CONSTANT_GET, CONSTANT_SET;
	
	static {
		var lookup = MethodHandles.lookup();
		try {
			// Guards
			CHECK_TABLE_SHAPE = lookup.findStatic(TableAccess.class, "checkTableShape",
					MethodType.methodType(boolean.class, Object.class, Object.class, Object.class));
			CHECK_TABLE_AND_META_SHAPES = lookup.findStatic(TableAccess.class, "checkTableAndMetaShapes",
					MethodType.methodType(boolean.class, Object.class, Object.class, Object.class, Object.class));
			
			// LuaTable accessors
			GET_ARRAY = MethodHandles.dropArguments(lookup.findVirtual(LuaTable.class, "getArray",
					MethodType.methodType(Object.class, int.class)), 0, Object.class);
			SET_ARRAY = MethodHandles.dropArguments(lookup.findVirtual(LuaTable.class, "setArray",
					MethodType.methodType(void.class, int.class, Object.class)), 0, Object.class);
			GET_AT = lookup.findVirtual(LuaTable.class, "getAt",
					MethodType.methodType(Object.class, int.class));
			SET_AT = lookup.findVirtual(LuaTable.class, "setAt",
					MethodType.methodType(void.class, int.class, Object.class, Object.class));
			GET_RAW = MethodHandles.dropArguments(lookup.findVirtual(LuaTable.class, "getRaw",
					MethodType.methodType(Object.class, Object.class)), 0, Object.class);
			SET_RAW = MethodHandles.dropArguments(lookup.findVirtual(LuaTable.class, "setRaw",
					MethodType.methodType(void.class, Object.class, Object.class)), 0, Object.class);
			GET = MethodHandles.dropArguments(lookup.findVirtual(LuaTable.class, "get",
					MethodType.methodType(Object.class, Object.class)), 0, Object.class);
			SET = MethodHandles.dropArguments(lookup.findVirtual(LuaTable.class, "set",
					MethodType.methodType(void.class, Object.class, Object.class)), 0, Object.class);
		} catch (NoSuchMethodException | IllegalAccessException e) {
			throw new AssertionError(e);
		}
		
		CONSTANT_GET = TableAccess::resolveConstantGet;
		CONSTANT_SET = TableAccess::resolveConstantSet;
	}
	
	private static LuaCallTarget resolveConstantGet(LuaCallSite meta, Object[] args) {
		if (meta.linkageCount > MAX_LINKAGE_COUNT) {
			// Slow path: table seems to be changing too often
			// TODO try to still optimize stable, shared metatable; this is used for Lua "OOP"
			return new LuaCallTarget(GET);
		}
		
		var key = args[1];
		if (args[0] instanceof LuaTable table) {
			var metatable = table.getMetatable();
			var arrayIndex = table.getArrayIndex(key);
			if (arrayIndex != -1 && (table.getArray(arrayIndex) != null || metatable == null)) {
				// Fast path: read value from array
				return new LuaCallTarget(GET_ARRAY, CHECK_TABLE_SHAPE.bindTo(table));
			}
			
			var slot = table.getSlot(key);
			if (slot != -1) {
				// Fast path: key is present in table -> use slot-based access
				// (absent keys don't have a slot, but might receive one without table shape changing)
				var target = MethodHandles.dropArguments(MethodHandles.insertArguments(GET_AT, 0, table, slot),
						0, Object.class, Object.class, Object.class);
				return new LuaCallTarget(target, CHECK_TABLE_SHAPE.bindTo(table.shape));
			} else {
				// We need to check the metatable
				if (metatable.metatable == null) {
					// Fast path: metatable that does not itself have a metatable
					var index = metatable.get("__index");
					if (index instanceof LuaTable fallbackTbl) {
						// Slow path: TODO implement the fast path to __index table
						// (preferably with support for a chain of __index with metatables)
						return new LuaCallTarget(GET, CHECK_TABLE_SHAPE.bindTo(table.shape));
					} else if (index != null) {
						// Link a call into the __index method
						var types = new LuaType[] {LuaType.UNKNOWN, LuaType.UNKNOWN};
						var target = LuaLinker.linkCall(new LuaCallSite(meta.site, new CallSiteOptions(types, false, false)), index, table, key);
						var guard = MethodHandles.insertArguments(CHECK_TABLE_AND_META_SHAPES, 0, table.shape, metatable.shape);
						return target.withGuards(guard);
					} else {
						// Slow path: not in table, missing __index
						// this won't become optimized even if key is inserted, because shape doesn't change!
						// TODO always return null, but use a guard that detects 1) if key was inserted and 2) both shape changes
						var guard = MethodHandles.insertArguments(CHECK_TABLE_AND_META_SHAPES, 0, table.shape, metatable.shape);
						return new LuaCallTarget(GET, guard);
					}
				} else {
					// Slow path: metatables on top of more metatables
					// TODO technically, this might not need a guard - but a guard allows re-optimizing later
					return new LuaCallTarget(GET, CHECK_TABLE_SHAPE.bindTo(table.shape));
				}
			}
		} else {
			throw new UnsupportedOperationException("userdata");
		}
	}
	
	private static LuaCallTarget resolveConstantSet(LuaCallSite meta, Object[] args) {
		// TODO implement some optimizations for write path before this is used
		// Right now this would just slow down the write path
		return new LuaCallTarget(SET);
	}
	
	@SuppressWarnings("unused") // MethodHandle
	private static boolean checkTableShape(Object expectedShape, Object callable, Object tbl) {
		if (tbl instanceof LuaTable table) {
			return expectedShape == table.shape;
		}
		return false;
	}
	
	@SuppressWarnings("unused") // MethodHandle
	private static boolean checkTableAndMetaShapes(Object expectedShape, Object expectedMetaShape, Object callable, Object tbl) {
		if (tbl instanceof LuaTable table) {
			return expectedShape == table.shape && expectedMetaShape == table.metatable.shape;
		}
		return false;
	}
}
