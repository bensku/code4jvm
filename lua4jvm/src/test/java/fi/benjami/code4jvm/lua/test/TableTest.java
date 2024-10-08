package fi.benjami.code4jvm.lua.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fi.benjami.code4jvm.lua.LuaVm;
import fi.benjami.code4jvm.lua.debug.LinkerTrace;
import fi.benjami.code4jvm.lua.debug.LuaDebugOptions;
import fi.benjami.code4jvm.lua.runtime.LuaFunction;
import fi.benjami.code4jvm.lua.runtime.LuaTable;
import fi.benjami.code4jvm.lua.runtime.TableAccess;

public class TableTest {

	private LuaVm vm;
	private LinkerTrace trace;
	
	@BeforeEach
	public void beginTrace() {
		vm = new LuaVm();
		trace = new LinkerTrace();
		LuaDebugOptions.linkerTrace = trace;
	}
	
	@Test
	public void simpleAccess() {
		var table = new LuaTable();
		table.set("foo", "bar");
		assertEquals("bar", table.get("foo"));
		table.set("foo", null);
		assertNull(table.get("foo"));
	}
	
	@Test
	public void arrayTable() {
		var table = new LuaTable();
		
		// Initialize array part
		table.set(1d, "foo");
		table.set(2d, "bar");
		table.set(3d, "baz");
		assertEquals("foo", table.get(1d));
		assertEquals("bar", table.get(2d));
		assertEquals("baz", table.get(3d));
		assertEquals(3, table.arraySize());
		
		// Initialize table part
		table.set("hello", "world");
		assertEquals("foo", table.get(1d));
		assertEquals("bar", table.get(2d));
		assertEquals("baz", table.get(3d));
		assertEquals(3, table.arraySize());
		assertEquals("world", table.get("hello"));
		
		// Enlarge array
		table.set(4d, "1");
		table.set(5d, "2");
		assertEquals(5, table.arraySize());
		assertEquals("world", table.get("hello"));
		
		// Enlarge table
		table.set("foo", "a");
		table.set("bar", "b");
		table.set("baz", "c");
		table.set("???", "end");
		
		// Check that everything is still in order
		assertEquals("foo", table.get(1d));
		assertEquals("bar", table.get(2d));
		assertEquals("baz", table.get(3d));
		assertEquals("1", table.get(4d));
		assertEquals("2", table.get(5d));
		assertEquals(5, table.arraySize());
		assertEquals("world", table.get("hello"));
		assertEquals("a", table.get("foo"));
		assertEquals("b", table.get("bar"));
		assertEquals("c", table.get("baz"));
		assertEquals("end", table.get("???"));
	}
	
	@Test
	public void constantGet() throws Throwable {
		var result = vm.execute("""
				local tbl = {}
				tbl.foo = "bar"
				return tbl.foo
				""");
		assertEquals("bar", result);
		assertEquals(1, trace.metadata.linkageCount);
		assertEquals(TableAccess.CONSTANT_GET, trace.callable);
	}
	
	@Test
	public void metatableIndex() throws Throwable {
		var table = new LuaTable();
		table.set("foo", "bar");
		var func = (LuaFunction) vm.execute("""
				return function (tbl)
					return tbl.foo
				end
				""");
		assertEquals("bar", func.call(table));
		assertEquals(1, trace.metadata.linkageCount);
		
		var meta = (LuaTable) vm.execute("""
				return {
					__index = function (tbl, key) 
						return "metatable!"
					end
				}
				""");
		table.metatable(meta);
		assertEquals("bar", func.call(table));
		assertEquals(2, trace.metadata.linkageCount);
		
		table.set("foo", null);
		assertEquals("metatable!", func.call(table));
		// Can't check linkage count, because that refers to the __index call
		
		table.set("foo", "new");
		assertEquals("new", func.call(table));
		assertEquals(4, trace.metadata.linkageCount);
		
		table.set("foo", null);
		assertEquals("metatable!", func.call(table));
		// Fall of the fast path: link to LuaTable#get(...), which links call to __index
		
		table.set("foo", "new2");
		assertEquals("new2", func.call(table));
		// Since we fell of the fast path, last linked call is still the one to __index
		// (not that we call it now, but call to get(...) was linked before it and is here to stay)
		assertInstanceOf(LuaFunction.class, trace.callable);
	}
	
	@Test
	public void metatableNewIndex() throws Throwable {
		var table = new LuaTable();
		table.set("foo", "bar");
		var func = (LuaFunction) vm.execute("""
				return function (tbl, val)
					tbl.foo = val
				end
				""");
		func.call(table, "test");
		assertEquals("test", table.get("foo"));
		
		var outTable = new LuaTable();
		vm.globals().set("outTable", outTable);
		var meta = (LuaTable) vm.execute("""
				local out = outTable
				return {
					__newindex = function (tbl, key, value) 
						out[key] = value
					end
				}
				""");
		table.metatable(meta);
		
		// Existing key ignores __newindex
		func.call(table, "test2");
		assertEquals("test2", table.get("foo"));
		table.set("foo", "test3");
		assertEquals("test3", table.get("foo"));
		assertEquals(null, outTable.get("foo"));
		
		func.call(table, null);
		assertEquals(null, table.get("foo"));
		func.call(table, "bar");
		assertEquals(null, table.get("foo"));
		assertEquals("bar", outTable.get("foo"));
	}
	
	@Test
	public void specializeIndex() throws Throwable {
		// Type changes shouldn't break anything
		var table = new LuaTable();
		var func = (LuaFunction) vm.execute("""
				return function (tbl, field)
					return tbl[field]
				end
				""");
		var meta = (LuaTable) vm.execute("""
				return {
					__index = function (tbl, key) 
						return key
					end
				}
				""");
		table.metatable(meta);
		
		assertEquals(1.0d, func.call(table, 1.0d));
		assertEquals("foo", func.call(table, "foo"));
		assertEquals(3.0d, func.call(table, 3.0d));
	}
	
	@Test
	public void mutableMetatable() throws Throwable {
		var table = new LuaTable();
		var func = (LuaFunction) vm.execute("""
				return function (tbl)
					return tbl.foo
				end
				""");
		var index1 = vm.execute("""
				return function (tbl, key)
					return "meta1"
				end
				""");
		var index2 = vm.execute("""
				return function (tbl, key)
					return "meta2"
				end
				""");
		var meta = new LuaTable();
		table.metatable(meta);
		
		assertNull(func.call(table));
		
		meta.set("__index", index1);
		assertEquals("meta1", func.call(table));
		
		meta.set("__index", index2);
		assertEquals("meta2", func.call(table));

		meta.set("__index", index1);
		assertEquals("meta1", func.call(table));
		
		meta.set("__index", null);
		assertNull(func.call(table));
	}
	
	@Test
	public void tableIterators() {
		var table = new LuaTable();
		table.set(1d, "test");
		table.set("foo", 1d);
		table.set("bar", 2d);
		table.set("baz", 3d);
		
		var itKeys = new HashSet<>();
		var itVals = new HashSet<>();
		var nextKeys = new HashSet<>();
		var nextVals = new HashSet<>();
		
		var it = table.iterator();
		while (it.next()) {
			itKeys.add(it.key());
			itVals.add(it.value());
		}
		
		Object prevKey = null;
		do {
			var entry = table.next(prevKey);
			if (entry != null) {
				prevKey = entry[0];
				nextKeys.add(entry[0]);
				nextVals.add(entry[1]);
			} else {
				prevKey = null;
			}
		} while (prevKey != null);
		
		var keys = Set.of(1, "foo", "bar", "baz");
		var values = Set.of("test", 1d, 2d, 3d);
		
		assertEquals(keys, itKeys);
		assertEquals(values, itVals);
		assertEquals(keys, nextKeys);
		assertEquals(values, nextVals);
	}
	
	@Test
	public void arrayIterator() {
		var table = new LuaTable();
		table.set(1d, "test");
		table.set(2d, "second");
		table.set(4d, "third"); // This is after gap, shouldn't be visible!
		table.set("foo", 1d);
		table.set("bar", 2d);
		table.set("baz", 3d);
		
		{
			var itKeys = new HashSet<>();
			var itVals = new HashSet<>();
			
			var it = table.arrayIterator();
			while (it.next()) {
				itKeys.add(it.key());
				itVals.add(it.value());
			}
			
			var keys = Set.of(1, 2);
			var values = Set.of("test", "second");
			
			assertEquals(keys, itKeys);
			assertEquals(values, itVals);
		}
		
		table.set(3d, "later!"); // This should close the gap
		
		{
			var itKeys = new HashSet<>();
			var itVals = new HashSet<>();
			
			var it = table.arrayIterator();
			while (it.next()) {
				itKeys.add(it.key());
				itVals.add(it.value());
			}
			
			var keys = Set.of(1, 2, 3, 4);
			var values = Set.of("test", "second", "later!", "third");
			
			assertEquals(keys, itKeys);
			assertEquals(values, itVals);
		}
	}
	
	@Test
	public void fakeArray() {
		// If there were too big gaps in table for it to be array,
		// filling those gaps should still make the entries visible
		var table = new LuaTable();
		table.set(1d, "test");
		table.set(10d, "second");
		table.set(100d, "third");
		
		{
			var it = table.arrayIterator();
			assertTrue(it.next());
			assertEquals(1, it.key());
			assertEquals("test", "test");
			assertFalse(it.next());
		}
		
		for (double i = 2; i < 10; i++) {
			table.set(i, i);
		}
		
		{
			var itKeys = new HashSet<>();
			var itVals = new HashSet<>();
			
			var it = table.arrayIterator();
			while (it.next()) {
				itKeys.add(it.key());
				itVals.add(it.value());
			}
			
			var keys = Set.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
			var values = Set.of("test", "second", 2d, 3d, 4d, 5d, 6d, 7d, 8d, 9d);
			
			assertEquals(keys, itKeys);
			assertEquals(values, itVals);
		}
	}
	
	@Test
	public void enlargeArray() {
		// Wrong multiplier for array size may cause AOOBE in edge cases like this
		var table = new LuaTable();
		table.set(1d, "test");
		table.set(5d, "second");
	}
}
