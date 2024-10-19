package fi.benjami.code4jvm.lua.test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import fi.benjami.code4jvm.lua.LuaVm;
import fi.benjami.code4jvm.lua.ffi.Fallback;
import fi.benjami.code4jvm.lua.ffi.LuaBinder;
import fi.benjami.code4jvm.lua.ffi.LuaExport;
import fi.benjami.code4jvm.lua.stdlib.LuaException;

public class BinderTest {
	
	private final LuaBinder BINDER = new LuaBinder(MethodHandles.lookup());

	private static class LuaApi {
		
		@LuaExport("noop")
		public static void noop() {}
		
		@LuaExport("returnArg")
		public static Object returnArg(Object arg) {
			return arg;
		}
		
		@LuaExport("sum")
		public static double sum(double a, double b) {
			return a + b;
		}
		
		@LuaExport("test")
		public static String testOverload(String a, String b) {
			return "str1";
		}
		
		@LuaExport("test")
		public static String testOverload(String s) {
			return "str2";
		}
		
		@LuaExport("test")
		public static String testOverload(double d) {
			return "num";
		}
		
		@LuaExport("test")
		@Fallback
		public static String testOverload(Object o) {
			return "fallback";
		}
		
		@LuaExport("throwing1")
		public static String throwing1(String s) {
			throw new NullPointerException();
		}
		
		@LuaExport("throwing2")
		public static String throwing2(String s) {
			throw new ClassCastException();
		}
		
		@LuaExport("throwing3")
		public static String throwing3(String s) throws IOException {
			throw new IOException();
		}
	}
	
	@Test
	public void bindFunctions() throws Throwable {
		var funcs = BINDER.bindFunctionsFrom(LuaApi.class);
		
		// Install functions to globals
		var vm = new LuaVm();
		for (var func : funcs) {
			vm.globals().set(func.name(), func);
		}
		
		vm.execute("noop()");
		assertNull(vm.execute("return noop()"));
		
		assertDoesNotThrow(() -> vm.execute("noop(123)"));
		
		assertEquals(12.5, vm.execute("return sum(5, 7.5)"));
		assertEquals(12.5, vm.execute("""
				five = 5 -- Store to globals
				return sum(five, 7.5)
				"""));
		assertThrows(LuaException.class, () -> vm.execute("return sum(5, \"foo\")"));
		assertThrows(LuaException.class, () -> vm.execute("""
				str = "foo"
				return sum(str, 7.5)
				"""));
		
		// Ensure we pick correct overloads
		assertEquals("str1", vm.execute("""
				return test("foo", "bar")
				"""));
		assertEquals("str1", vm.execute("""
				a = "foo"
				b = "bar"
				return test(a, b)
				"""));
		assertEquals("str2", vm.execute("""
				a = "foo"
				return test(a)
				"""));
		assertEquals("num", vm.execute("""
				a = 1
				return test(a)
				"""));
		// Note hoe the string is unused
		assertEquals("num", vm.execute("""
				a = 1
				b = "bar"
				return test(a, b)
				"""));
		assertEquals("fallback", vm.execute("""
				a = {}
				b = "bar"
				return test(a, b)
				"""));
		
		// Force runtime types (by using globals) - we're only doing exception magic with them
		assertThrows(NullPointerException.class, () -> vm.execute("""
				str = "foo"
				throwing1(str)
				"""));
		assertThrows(ClassCastException.class, () -> vm.execute("""
				str = "foo"
				throwing2(str)
				"""));
		assertThrows(IOException.class, () -> vm.execute("""
				str = "foo"
				throwing3(str)
				"""));
	}
	
	private static class LuaVarargs {
		@LuaExport("checkArgs")
		public static boolean checkArgs(Object... args) {
			return Arrays.equals(args, new Object[] {"foo", "bar", 3, "baz"});
		}
		
		@LuaExport("returnArgs")
		public static Object[] returnArgs(Object... args) {
			return args;
		}
		
		@LuaExport("returnArgs2")
		public static Object[] returnArgs2(double dummy, Object... args) {
			return args;
		}
	}
	
	@Test
	public void varargs() throws Throwable {
		var funcs = BINDER.bindFunctionsFrom(LuaVarargs.class);
		
		// Install functions to globals
		var vm = new LuaVm();
		for (var func : funcs) {
			vm.globals().set(func.name(), func);
		}
		
		assertTrue((boolean) vm.execute("""
				return checkArgs("foo", "bar", 3, "baz")
				"""));
		assertArrayEquals(new Object[] {"foo", "bar", 3, "baz"}, (Object[]) vm.execute("""
				return returnArgs("foo", "bar", 3, "baz")
				"""));
		assertArrayEquals(new Object[] {"foo", "bar", 3.1, "baz"}, (Object[]) vm.execute("""
				return returnArgs("foo", "bar", 3.1, "baz")
				"""));
		assertArrayEquals(new Object[] {"foo", "bar", 3, "baz"}, (Object[]) vm.execute("""
				return returnArgs2(123, "foo", "bar", 3, "baz")
				"""));
		assertArrayEquals(new Object[] {"foo", "bar", 3.1, "baz"}, (Object[]) vm.execute("""
				return returnArgs2(123, "foo", "bar", 3.1, "baz")
				"""));
	}
}
