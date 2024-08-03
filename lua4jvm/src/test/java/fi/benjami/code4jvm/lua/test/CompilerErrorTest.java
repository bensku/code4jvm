package fi.benjami.code4jvm.lua.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

import fi.benjami.code4jvm.lua.LuaVm;
import fi.benjami.code4jvm.lua.compiler.LuaSyntaxException;

public class CompilerErrorTest {

	private final LuaVm vm = new LuaVm();
	
	@Test
	public void syntaxError() {
		try {
			vm.compile("\nfunction #() end");
		} catch (LuaSyntaxException e) {
			var errors = e.errors();
			assertEquals(1, errors.size());
			var err = errors.get(0);
			assertEquals("unknown", err.chunkName());
			assertEquals(2, err.line());
			assertEquals(9, err.posInLine());
			// Don't check the message; it is currently whatever antlr 4 sees fit
			return;
		}
		fail();
	}
	
	@Test
	public void multipleErrors() {
		try {
			vm.compile("""
					function #() end
					
					function #() end
					""");
		} catch (LuaSyntaxException e) {
			var errors = e.errors();
			assertEquals(2, errors.size());
			var first = errors.get(0);
			assertEquals("unknown", first.chunkName());
			assertEquals(1, first.line());
			assertEquals(9, first.posInLine());
			
			var second = errors.get(1);
			assertEquals("unknown", second.chunkName());
			assertEquals(3, second.line());
			assertEquals(9, second.posInLine());
			return;
		}
		fail();
	}
}
