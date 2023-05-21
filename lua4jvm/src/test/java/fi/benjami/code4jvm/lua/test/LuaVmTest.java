package fi.benjami.code4jvm.lua.test;

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
	public void emptyProgram() throws Throwable {
		// TODO implicit nil return
		vm.execute("return");
	}
}
