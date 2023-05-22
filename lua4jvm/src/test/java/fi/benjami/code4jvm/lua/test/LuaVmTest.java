package fi.benjami.code4jvm.lua.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import fi.benjami.code4jvm.lua.LuaVm;
import fi.benjami.code4jvm.lua.parser.LuaLexer;
import fi.benjami.code4jvm.lua.parser.LuaNode;
import fi.benjami.code4jvm.lua.parser.LuaToken;
import fi.benjami.code4jvm.lua.parser.LuaTokenTransformer;
import fi.benjami.code4jvm.lua.runtime.LuaFunction;
import fi.benjami.parserkit.parser.Parser;

public class LuaVmTest {

	// Create parser only once, it is relatively slow to compile
	private static final Parser PARSER = Parser.compileAndLoad(LuaNode.REGISTRY, LuaToken.values());
	private final LuaVm vm = new LuaVm(new LuaLexer(), new LuaTokenTransformer(), PARSER);
	
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
}
