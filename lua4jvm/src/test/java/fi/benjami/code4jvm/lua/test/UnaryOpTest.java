package fi.benjami.code4jvm.lua.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import fi.benjami.code4jvm.lua.LuaVm;
import fi.benjami.code4jvm.lua.runtime.LuaTable;
import fi.benjami.code4jvm.lua.stdlib.LuaException;

public class UnaryOpTest {

	private final LuaVm vm = new LuaVm();
	
	@Test
	public void negateNumbers() throws Throwable {
		assertEquals(-10d, vm.execute("return -10"));
		assertEquals(10d, vm.execute("return -(-10)"));
		assertEquals(-10d, vm.execute("""
				ten = 10
				return -ten
				"""));
	}
	
	@Test
	public void negateMetatable() throws Throwable {
		var metaTbl = new LuaTable();
		metaTbl.set("__unm", vm.execute("""
				return function (self)
					return "nope!"
				end
				"""));
		
		var tbl = new LuaTable();
		tbl.metatable(metaTbl);
		vm.globals().set("tbl", tbl);
		
		assertEquals("nope!", vm.execute("return -tbl"));
		metaTbl.set("__unm", null);
		assertThrows(LuaException.class, () -> vm.execute("return -tbl"));
	}
	
	@Test
	public void stringLength() throws Throwable {
		assertEquals(5d, vm.execute("return #\"12345\""));
		assertEquals(5d, vm.execute("""
				str = "12345"
				return #str
				"""));
	}
	
	@Test
	public void tableLength() throws Throwable {
		// Array length
		assertEquals(0d, vm.execute("return #{}"));
		assertEquals(5d, vm.execute("return #{1, 2, 3, false, true}"));
		assertEquals(0d, vm.execute("return #{foo = 1}"));
		
		// Metatables
		var metaTbl = new LuaTable();
		metaTbl.set("__len", vm.execute("""
				return function (self)
					return "nope!"
				end
				"""));
		
		var tbl = new LuaTable();
		tbl.metatable(metaTbl);
		vm.globals().set("tbl", tbl);
		
		assertEquals("nope!", vm.execute("return #tbl"));
		metaTbl.set("__len", null);
		assertEquals(0d, vm.execute("return #tbl"));
	}
}
