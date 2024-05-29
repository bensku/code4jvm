package fi.benjami.code4jvm.lua.test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import fi.benjami.code4jvm.lua.LuaVm;
import fi.benjami.code4jvm.lua.runtime.LuaFunction;

public class MultiValTest {

	@Test
	public void returnToJava() throws Throwable {
		var vm = new LuaVm();
		assertArrayEquals(new Object[] {"foo", 3d, "bar", "baz"}, (Object[]) vm.execute("""
				return "foo", 3, "bar", "baz"
				"""));
	}
	
	@Test
	public void returnToLua() throws Throwable {
		var vm = new LuaVm();
		vm.execute("""
				local function stuff()
					return "foo", 3, "bar", "baz"
				end
				a, b, c, d = stuff() -- assign to globals
				""");
		assertEquals("foo", vm.globals().get("a"));
		assertEquals(3d, vm.globals().get("b"));
		assertEquals("bar", vm.globals().get("c"));
		assertEquals("baz", vm.globals().get("d"));
	}
	
	@Test
	public void multiValSyntax() throws Throwable {
		// BinderTest handles calls to Java functions
		var vm = new LuaVm();

		// Just return the varargs
		var func = (LuaFunction) vm.execute("""
				return function (...)
					return ...
				end
				""");
		assertArrayEquals(new Object[] {"foo", "bar", 3d, "baz"},
				(Object[]) func.call("foo", "bar", 3d, "baz"));
		
		// Return the varargs but also something else
		var func2 = (LuaFunction) vm.execute("""
				return function (...)
					return "hello world", ...
				end
				""");
		assertArrayEquals(new Object[] {"hello world", "foo", "bar", 3d, "baz"},
				(Object[]) func2.call("foo", "bar", 3d, "baz"));
	}
	
	@Test
	public void nestedVarargs() throws Throwable {
		var vm = new LuaVm();

		var func = (LuaFunction) vm.execute("""
				local function test()
					return "foo", 3, "bar", "baz"
				end
				
				return function ()
					return test()
				end
				""");
		// Arguments given to the function should not matter
		assertArrayEquals(new Object[] {"foo", 3d, "bar", "baz"},
				(Object[]) func.call());
		assertArrayEquals(new Object[] {"foo", 3d, "bar", "baz"},
				(Object[]) func.call("foo", 3d, "bar", "baz"));
		
		// Same goes for arguments in Lua side
		var func2 = (LuaFunction) vm.execute("""
				local function test()
					return "foo", 3, "bar", "baz"
				end
				
				return function ()
					return test(1, "test", 3)
				end
				""");
		assertArrayEquals(new Object[] {"foo", 3d, "bar", "baz"},
				(Object[]) func2.call());
	}
	
	@Test
	public void assignMultiVal() throws Throwable {
		var vm = new LuaVm();
		var func = (LuaFunction) vm.execute("""
				return function (...)
					a, b, c, d = ...
				end
				""");
		func.call("foo", 3d, "bar", "baz");
		assertEquals("foo", vm.globals().get("a"));
		assertEquals(3d, vm.globals().get("b"));
		assertEquals("bar", vm.globals().get("c"));
		assertEquals("baz", vm.globals().get("d"));
	}
}