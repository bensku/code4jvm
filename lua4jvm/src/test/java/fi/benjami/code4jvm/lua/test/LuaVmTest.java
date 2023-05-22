package fi.benjami.code4jvm.lua.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import fi.benjami.code4jvm.lua.LuaVm;
import fi.benjami.code4jvm.lua.parser.LuaLexer;
import fi.benjami.code4jvm.lua.parser.LuaNode;
import fi.benjami.code4jvm.lua.parser.LuaToken;
import fi.benjami.code4jvm.lua.parser.LuaTokenTransformer;
import fi.benjami.parserkit.parser.Parser;

public class LuaVmTest {

	private final LuaVm vm = new LuaVm(new LuaLexer(), new LuaTokenTransformer(), Parser.compileAndLoad(LuaNode.REGISTRY, LuaToken.values()));
	
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
}
