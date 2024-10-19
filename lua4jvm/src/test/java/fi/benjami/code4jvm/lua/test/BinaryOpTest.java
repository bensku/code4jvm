package fi.benjami.code4jvm.lua.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import fi.benjami.code4jvm.internal.DebugOptions;
import fi.benjami.code4jvm.lua.LuaVm;
import fi.benjami.code4jvm.lua.runtime.LuaFunction;
import fi.benjami.code4jvm.lua.runtime.LuaTable;
import fi.benjami.code4jvm.lua.stdlib.LuaException;

public class BinaryOpTest {

	private final LuaVm vm = new LuaVm();
	
	@Test
	public void stringConcat() throws Throwable {
		assertEquals("foobarbaz", vm.execute("""
				return "foo" .. "bar" .. "baz"
				"""));
		assertEquals("foobarbaz", vm.execute("""
				foo = "foo" -- set to globals
				return foo .. "bar" .. "baz"
				"""));
	}
	
	@Test
	public void stringConcatMetatables() throws Throwable {
		var metaTbl = new LuaTable();
		metaTbl.set("__concat", vm.execute("""
				return function (a, b)
					return b
				end
				"""));
		
		var tbl = new LuaTable();
		tbl.metatable(metaTbl);
		
		var concat = (LuaFunction) vm.execute("""
				return function (lhs, rhs)
					return lhs .. rhs
				end
				""");
		
		assertEquals("foo", concat.call(tbl, "foo"));
		assertEquals(tbl, concat.call("foo", tbl));
		metaTbl.set("__concat", null); // causes site to be relinked
		assertThrows(LuaException.class, () -> concat.call("foo", tbl));
		assertThrows(LuaException.class, () -> concat.call(10.0, 1.0));
	}
	
	@Test
	public void simpleMath() throws Throwable {
		assertEquals(125d, vm.execute("return 5 ^ 3"));
		assertEquals(15, vm.execute("return 5 * 3"));
		assertEquals(17.5, vm.execute("return 5 * 3.5"));
		assertEquals(2.5, vm.execute("return 5 / 2"));
		assertEquals(2, vm.execute("return 5 // 2"));
		assertEquals(2, vm.execute("return 10 % 4"));
		assertEquals(2, vm.execute("return -10 % 4"));
		assertEquals(8, vm.execute("return 5 + 3"));
		assertEquals(8.4, vm.execute("return 5.4 + 3"));
		assertEquals(2, vm.execute("return 5 - 3"));
		assertEquals(1.1, vm.execute("return 5 - 3.9"));
		
		assertEquals(2, vm.execute("""
				lhs = 5
				return lhs - 3
				"""));
		assertEquals(2.5, vm.execute("""
				lhs = 5.5
				return lhs - 3
				"""));
	}
	
	@Test
	public void arithmeticMetatables() throws Throwable {
		var metamethod = vm.execute("""
				return function (a, b)
				return b
			end
			""");
		var metaTbl = new LuaTable();
		metaTbl.set("__pow", metamethod);
		metaTbl.set("__mul", metamethod);
		metaTbl.set("__div", metamethod);
		metaTbl.set("__idiv", metamethod);
		metaTbl.set("__mod", metamethod);
		metaTbl.set("__add", metamethod);
		metaTbl.set("__sub", metamethod);
		
		var tbl = new LuaTable();
		tbl.metatable(metaTbl);
		
		vm.globals().set("testTbl", tbl);
		
		// We've set all "arithmetic" operations to behave same; now, test them all!
		var ops = List.of("^", "*", "/", "//", "%", "+", "-");
		for (var op : ops) {			
			assertEquals("foo", vm.execute("return testTbl " + op + " \"foo\""));
			assertEquals(tbl, vm.execute("return \"foo\" " + op + " testTbl"));
		}
	}
}
