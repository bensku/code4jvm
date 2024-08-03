package fi.benjami.code4jvm.lua.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import fi.benjami.code4jvm.lua.LuaVm;
import fi.benjami.code4jvm.lua.stdlib.LuaException;

public class DebugInfoTest {

	private final LuaVm vm = new LuaVm();
	
	@Test
	public void mainBlock() throws Throwable {
		try {			
			vm.execute("dir/file.lua", """
				
				error("test")
				""");
		} catch (LuaException e) {
			var el =  e.getStackTrace()[0];
			assertEquals("file", el.getClassName());
			assertEquals("main chunk", el.getMethodName());
			assertEquals("dir/file.lua", el.getFileName());
			assertEquals(2, el.getLineNumber());
		}
	}
	
	@Test
	public void function() throws Throwable {
		try {			
			vm.execute("dir/file.lua", """
				function raiseError()
					error("test")
				end
				raiseError()
				""");
		} catch (LuaException e) {
			var inner = e.getStackTrace()[0];
			assertEquals("file", inner.getClassName());
			assertEquals("raiseError", inner.getMethodName());
			assertEquals("dir/file.lua", inner.getFileName());
			assertEquals(2, inner.getLineNumber());
			
			var outer = e.getStackTrace()[1];
			assertEquals("file", outer.getClassName());
			assertEquals("main chunk", outer.getMethodName());
			assertEquals("dir/file.lua", outer.getFileName());
			assertEquals(4, outer.getLineNumber());
		}
		
		// Now try anonymous function
		try {			
			vm.execute("dir/file.lua", """
				(function () error("test") end)()
				""");
		} catch (LuaException e) {
			var inner = e.getStackTrace()[0];
			assertEquals("file", inner.getClassName());
			assertEquals("anonymous", inner.getMethodName());
			assertEquals("dir/file.lua", inner.getFileName());
			assertEquals(1, inner.getLineNumber());
			
			var outer = e.getStackTrace()[1];
			assertEquals("file", outer.getClassName());
			assertEquals("main chunk", outer.getMethodName());
			assertEquals("dir/file.lua", outer.getFileName());
			assertEquals(1, outer.getLineNumber());
		}
	}
}
