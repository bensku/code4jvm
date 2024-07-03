package fi.benjami.code4jvm.lua.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.Test;

import fi.benjami.code4jvm.lua.LuaVm;
import fi.benjami.code4jvm.lua.VmOptions;
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
		assertEquals("1.0\t2.0\t3.0\n", new String(bas.toByteArray()));
	}
	
	@Test
	public void dofile() throws Throwable {
		vm.execute("dofile(\"src/test/resources/dofile.lua\")");
		assertTrue((boolean) vm.globals().get("DOFILE_SUCCESS"));
	}
}
