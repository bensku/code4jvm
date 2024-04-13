package fi.benjami.code4jvm.lua.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import fi.benjami.code4jvm.lua.LuaVm;
import fi.benjami.code4jvm.lua.ffi.JavaFunction;
import fi.benjami.code4jvm.lua.ir.LuaType;
import fi.benjami.code4jvm.lua.runtime.LuaFunction;
import fi.benjami.code4jvm.lua.runtime.LuaTable;

public class LuaVmTest {

	private final LuaVm vm = new LuaVm();
	
	@Test
	public void emptyModule() throws Throwable {
		assertNull(vm.execute("return"));
		assertNull(vm.execute("")); // Implicit return
	}
	
	@Test
	public void constantReturns() throws Throwable {
		assertEquals("abc", vm.execute("return 'abc'"));
		assertEquals("abc", vm.execute("return \"abc\""));
		assertEquals(10d, vm.execute("return 10"));
		assertEquals(true, vm.execute("return true"));
		assertEquals(false, vm.execute("return false"));
	}
	
	@Test
	public void declareFunction() throws Throwable {
		var func = (LuaFunction) vm.execute("""
				return function (a, b)
					return a + b
				end
				""");
		assertEquals(10.5d, func.call(4.5d, 6d));
		
		// Pass the declared function to another Lua function that calls it!
		var func2 = (LuaFunction) vm.execute("""
				return function (f)
					return f(1, 3.5)
				end
				""");
		assertEquals(4.5d, func2.call(func));
	}
	
			@Test
			public void createTable() throws Throwable {
				var empty = (LuaTable) vm.execute("""
						return {}
						""");
				assertEquals(0, empty.arraySize());
				
				var list = (LuaTable) vm.execute("""
						return {1, 2, 3}
						""");
				assertEquals(1d, list.get(1d));
				assertEquals(2d, list.get(2d));
				assertEquals(3d, list.get(3d));
				
				var foo = (LuaTable) vm.execute("""
						return {foo = "bar"}
						""");
				assertEquals("bar", foo.get("foo"));
				
				var tableCreator = (LuaFunction) vm.execute("""
						return function (key, value)
							return {[key] = value}
						end
						""");
				var dynamicTable = (LuaTable) tableCreator.call(3d, "bar");
				assertEquals("bar", dynamicTable.get(3d));
				
				var tableSetter = (LuaFunction) vm.execute("""
						return function (tbl, key, value)
							tbl.foo = {}
							tbl.foo.bar = {}
							tbl.foo.bar[1] = "baz"
							tbl[key] = value
							return tbl
					end
					""");
				var parent = (LuaTable) tableSetter.call(new LuaTable(), "test", "ok");
				assertEquals("ok", parent.get("test"));
				var fooTbl = (LuaTable) parent.get("foo");
				var barTbl = (LuaTable) fooTbl.get("bar");
				assertEquals("baz", barTbl.get(1d));
			}
	
	@Test
	public void complexMath() throws Throwable {
		assertEquals(2.0048d, vm.execute("return 1 * 2 + 3 / 5 ^ 2 ^ 2"));
		assertEquals(8d, vm.execute("return 2 * (3 + 2.5) - 3"));
	}
	
	@Test
	public void doEnd() throws Throwable {
		vm.execute("do do end end");
	}
	
	@Test
	public void ifBlock() throws Throwable {
		assertEquals(true, vm.execute("""
				if true then
					return true
				else
					return false
				end
				"""));
		assertEquals(false, vm.execute("""
				if 1 == 2 then
					return true
				else
					return false
				end
				"""));
		var func = (LuaFunction) vm.execute("""
				return function (a, b)
					if a > b then
						return "a"
					elseif a < b then
						return "b"
					elseif a == b then
						return "c"
					end
				end
				""");
		assertEquals("a", func.call(2d, 1d));
		assertEquals("b", func.call(2d, 3d));
		assertEquals("c", func.call(2d, 2d));
		
		var func2 = (LuaFunction) vm.execute("""
				return function (a, b)
					if a ~= b then
						return "a"
					else
						return "b"
					end
				end
				""");
		assertEquals("a", func2.call(1d, 2d));
		assertEquals("a", func2.call("foo", "bar"));
		assertEquals("b", func2.call("foo", "foo"));
		assertEquals("b", func2.call(func, func));
		
		var func3 = (LuaFunction) vm.execute("""
				return function (a, b, c)
					if a == b and b == c then
						return "a"
					else
						return "b"
					end
				end
				""");
		assertEquals("a", func3.call(1d, 1d, 1d));
		assertEquals("b", func3.call("", 1d, 2d));
		
		var func4 = (LuaFunction) vm.execute("""
				return function (a, b, c, d)
					if a == b and b == c or b == c and c == d then
						return "a"
					else
						return "b"
					end
				end
				""");
		assertEquals("a", func4.call(1d, 1d, 1d, 1d));
		assertEquals("b", func4.call("", 1d, 2d, 3d));
		assertEquals("a", func4.call("foo", "bar", "bar", "bar"));
		
		var func5 = (LuaFunction) vm.execute("""
				return function (a)
					if a ~= nil then
						return "a"
					else
						return "b"
					end
				end
				""");
		assertEquals("a", func5.call(new Object()));
		assertEquals("b", func5.call(new Object[] {null}));
	}
	
	@Test
	public void conditionalLoops() throws Throwable {
		assertEquals(10d, vm.execute("""
				local a = 0
				while a < 10 do
					a = a + 1
				end
				return a
				"""));
		assertEquals(10d, vm.execute("""
				local a = 0
				repeat
					a = a + 1
				until a >= 10
				return a
				"""));
	}
	
	@Test
	public void callJavaFunction() throws Throwable {
		Function<String, String> javaFunc = str -> str + str;
		var handle = MethodHandles.lookup().findVirtual(Function.class, "apply",
				MethodType.methodType(Object.class, Object.class)).bindTo(javaFunc);
		var callable = new JavaFunction("javaFunc", List.of(
				new JavaFunction.Target(List.of(), List.of(new JavaFunction.Arg("str", LuaType.STRING)), false, LuaType.STRING, false, handle)
		), null);
		var func = (LuaFunction) vm.execute("""
				return function (f, arg)
					return f(arg)
				end
				""");
		func.call(callable, "a");
		
//		assertEquals("10.5", vm.execute("""
//				return tostring(10.5)
//				"""));
//		assertEquals("number", vm.execute("""
//				return type(10.5)
//				"""));
	}
	
	@Test
	public void stringConcat() throws Throwable {
		var func = (LuaFunction) vm.execute("""
				return function (a, b)
					return a..b
				end
				""");
		assertEquals("foobar", func.call("foo", "bar"));
		
		var func2 = (LuaFunction) vm.execute("""
				return function (a, b)
					return a..b.."baz"
				end
				""");
		assertEquals("foobarbaz", func2.call("foo", "bar"));
	}
	
	@Test
	public void localFunction() throws Throwable {
		var func = (LuaFunction) vm.execute("""
				local function f(a, b)
					return a + b
				end
				return f
				""");
		assertEquals(10d, func.call(4d, 6d));
	}
	
	@Test
	public void upvalueFunction() throws Throwable {
		var func = (LuaFunction) vm.execute("""
				local function f(a, b)
					return a + b
				end
				local function g(a, b)
					return f(a, b) + 3
				end
				return g
				""");
		assertEquals(13d, func.call(4d, 6d));
	}
}
