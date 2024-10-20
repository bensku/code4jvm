package fi.benjami.code4jvm.lua.test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import fi.benjami.code4jvm.lua.LuaVm;
import fi.benjami.code4jvm.lua.VmOptions;
import fi.benjami.code4jvm.lua.runtime.LuaFunction;
import fi.benjami.code4jvm.lua.runtime.LuaTable;
import fi.benjami.code4jvm.lua.stdlib.LuaException;

public class BasicLibTest {

	private final LuaVm vm = new LuaVm();
	
	@Test
	public void errors() {
		assertThrows(LuaException.class, () -> vm.execute("error(\"foo\")"));
	}
	
	@Test
	public void metatables() throws Throwable {
		var tbl = new LuaTable();
		var metaTbl = new LuaTable();
		vm.globals().set("tbl", tbl);
		vm.globals().set("metaTbl", metaTbl);
		assertEquals(tbl, vm.execute("return setmetatable(tbl, metaTbl)"));
		assertEquals(metaTbl, tbl.metatable());
		assertEquals(metaTbl, vm.execute("return getmetatable(tbl)"));
		assertEquals(tbl, vm.execute("return setmetatable(tbl, nil)"));
		assertEquals(null, tbl.metatable());
		assertEquals(null, vm.execute("return getmetatable(tbl)"));
		
		assertEquals(null, vm.execute("getmetatable(nil)"));
	}
	
	@Test
	public void print() throws Throwable {
		var bas = new ByteArrayOutputStream();
		var out = new PrintStream(bas);
		var vm = new LuaVm(VmOptions.builder().stdOut(out).build());
		
		vm.execute("print(1, 2, 3)");
		vm.execute("print(1.1, 2.2, 3.3)");
		assertEquals("1\t2\t3\n1.1\t2.2\t3.3\n", new String(bas.toByteArray()));
	}
	
	@Test
	public void load() throws Throwable {
		{			
			var func = (LuaFunction) vm.execute("return load('LOAD_1 = true')");
			func.call();
			assertEquals(true, vm.globals().get("LOAD_1"));
		}
		{			
			var func = (LuaFunction) vm.execute("return load('LOAD_2 = true', 'test')");
			func.call();
			assertEquals(true, vm.globals().get("LOAD_2"));
		}
		{			
			var func = (LuaFunction) vm.execute("return load('LOAD_3 = true', 'test', 't')");
			func.call();
			assertEquals(true, vm.globals().get("LOAD_3"));
		}
		assertThrows(UnsupportedOperationException.class, () -> vm.execute("return load('LOAD_3 = true', 'test', 'b')"));
		{
			var newEnv = new LuaTable();
			vm.globals().set("NEW_ENV", newEnv);
			var func = (LuaFunction) vm.execute("return load('LOAD_4 = true', 'test', 't', NEW_ENV)");
			func.call();
			assertEquals(true, newEnv.get("LOAD_4"));
		}
	}
	
	@Test
	public void loadfile() throws Throwable {
		assertThrows(LuaException.class, () -> vm.execute("loadfile(\"src/test/resources/loadfile.lua\")"));
		
		var trustedVm = new LuaVm(VmOptions.builder()
				.fileSystem(FileSystems.getDefault())
				.stdIn(Files.newInputStream(Path.of("src/test/resources/loadfile.lua")))
				.build()
		);
		trustedVm.execute("loadfile(\"src/test/resources/loadfile.lua\")()");
		assertTrue((boolean) trustedVm.globals().get("LOADFILE_SUCCESS"));
		trustedVm.globals().set("LOADFILE_SUCCESS", null);
		
		// Test loading from "stdin"
		trustedVm.execute("loadfile()()");
		assertTrue((boolean) trustedVm.globals().get("LOADFILE_SUCCESS"));
	}
	
	@Test
	public void dofile() throws Throwable {
		assertThrows(LuaException.class, () -> vm.execute("dofile(\"src/test/resources/dofile.lua\")"));
		
		var trustedVm = new LuaVm(VmOptions.builder()
				.fileSystem(FileSystems.getDefault())
				.build()
		);
		trustedVm.execute("dofile(\"src/test/resources/dofile.lua\")");
		assertTrue((boolean) trustedVm.globals().get("DOFILE_SUCCESS"));
	}
	
	@Test
	public void pairs() throws Throwable {
		vm.execute("""
				tbl = {foo = 1, bar = 2, baz = 3}
				out = {}
				for k,v in pairs(tbl) do
					out[k] = v
				end
				""");
		var out = (LuaTable) vm.globals().get("out");
		assertEquals(1, out.get("foo"));
		assertEquals(2, out.get("bar"));
		assertEquals(3, out.get("baz"));
	}
	
	@Test
	public void pairs2() throws Throwable {
		vm.execute("""
				tbl = {"foo", "bar", "baz", foo = 1, bar = 2, baz = 3}
				out = {}
				for k,v in pairs(tbl) do
					out[k] = v
				end
				""");
		var out = (LuaTable) vm.globals().get("out");
		assertEquals(1, out.get("foo"));
		assertEquals(2, out.get("bar"));
		assertEquals(3, out.get("baz"));
	}
	
	@Test
	public void pairs3() throws Throwable {
		vm.execute("""
				tbl = {"foo", "bar", "baz", foo = 1.1, bar = 2.2, baz = 3.3}
				out = {}
				for k,v in pairs(tbl) do
					out[k] = v
				end
				""");
		var out = (LuaTable) vm.globals().get("out");
		assertEquals(1.1, out.get("foo"));
		assertEquals(2.2, out.get("bar"));
		assertEquals(3.3, out.get("baz"));
	}
	
	@Test
	public void next() throws Throwable {
		var results = (Object[]) vm.execute("""
				tbl = {foo = "bar"}
				local k1, v1 = next(tbl)
				local k2, v2 = next(tbl, k1)
				return k1, v1, k2, v2
				""");
		assertArrayEquals(new Object[] {"foo", "bar", null, null}, results);
	}
	
	@Test
	public void ipairs() throws Throwable {
		vm.execute("""
				tbl = {"foo", "bar", "baz", test = 1, second = 2}
				out = {}
				for k,v in ipairs(tbl) do
					out[#out+1] = v
				end
				""");
		var out = (LuaTable) vm.globals().get("out");
		assertEquals("foo", out.get(1d));
		assertEquals("bar", out.get(2d));
		assertEquals("baz", out.get(3d));
		assertEquals(null, out.get("test"));
		assertEquals(null, out.get("second"));
	}
	
	@Test
	public void pcallTest() throws Throwable {
		{			
			var result = (Object[]) vm.execute("return pcall(error, \"foo\")");
			assertArrayEquals(new Object[] {false, "foo"}, result);
		}
		
		{
			var result = (Object[]) vm.execute("""
					local function raiseError()
						error("foo123")
					end
					
					return pcall(raiseError)
					""");
			assertArrayEquals(new Object[] {false, "foo123"}, result);
		}
		
		{
			var result = (Object[]) vm.execute("""
					local function noError(arg)
						return "ok", arg, "bar"
					end
					
					return pcall(noError, "foo")
					""");
			assertArrayEquals(new Object[] {true, "ok", "foo", "bar"}, result);
		}
	}
	
	@Test
	public void nestedPcallTest() throws Throwable {
		var result = (Object[]) vm.execute("""
					local function raiseError()
						error("foo123")
					end
					
					return pcall(pcall, pcall, raiseError)
				""");
		assertArrayEquals(new Object[] {true, true, false, "foo123"}, result);
	}
}
