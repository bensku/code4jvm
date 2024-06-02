package fi.benjami.code4jvm.lua.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

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
}
