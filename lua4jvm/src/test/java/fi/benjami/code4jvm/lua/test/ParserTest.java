package fi.benjami.code4jvm.lua.test;

import java.io.IOException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import fi.benjami.code4jvm.lua.parser.LuaNode;
import fi.benjami.code4jvm.lua.parser.LuaToken;
import fi.benjami.code4jvm.lua.parser.SpecialNodes;
import fi.benjami.parserkit.parser.Parser;

@TestInstance(Lifecycle.PER_CLASS)
public class ParserTest {

	private Parser parser;
	
	@BeforeAll
	public void init() throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException, IOException {
//		DebugOptions.PRINT_METHODS = true;
		parser = Parser.compileAndLoad(LuaNode.REGISTRY, LuaToken.values());
		System.out.println(Parser.compile("foo", LuaNode.REGISTRY, LuaToken.values()).length / 1024);
//		parser.getClass().getField("HOOK").set(null, new TestParserHook());
	}
	
	@Test
	public void emptyChunk() {
		// TODO need lexer to test
		//parser.parse(SpecialNodes.Chunk.class, null);
	}
}
