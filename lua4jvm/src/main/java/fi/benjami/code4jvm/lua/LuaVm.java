package fi.benjami.code4jvm.lua;

import java.util.List;

import fi.benjami.code4jvm.lua.ir.LuaLocalVar;
import fi.benjami.code4jvm.lua.ir.LuaType;
import fi.benjami.code4jvm.lua.ir.UpvalueTemplate;
import fi.benjami.code4jvm.lua.parser.SpecialNodes;
import fi.benjami.code4jvm.lua.runtime.LuaFunction;
import fi.benjami.code4jvm.lua.runtime.LuaTable;
import fi.benjami.code4jvm.lua.semantic.LuaScope;
import fi.benjami.parserkit.lexer.Lexer;
import fi.benjami.parserkit.lexer.TokenTransformer;
import fi.benjami.parserkit.lexer.TokenizedText;
import fi.benjami.parserkit.parser.Parser;

public class LuaVm {

	private final Lexer lexer;
	private final TokenTransformer transformer;
	private final Parser parser;
	
	private final LuaTable globals;
	
	public LuaVm(Lexer lexer, TokenTransformer transformer, Parser parser) {
		this.lexer = lexer;
		this.transformer = transformer;
		this.parser = parser;
		this.globals = initGlobals();
	}
	
	private static LuaTable initGlobals() {
		var globals = new LuaTable(0);
		
		globals.set("_G", globals);
		globals.set("_VERSION", "lua4jvm DEV (Lua 5.4)");
		
		globals.set("print", LuaStdLib.PRINT);
		globals.set("tostring", LuaStdLib.TO_STRING);
		globals.set("type", LuaStdLib.TYPE);
		globals.set("tonumber", LuaStdLib.TO_NUMBER);
		
		return globals;
	}
	
	public LuaTable globals() {
		return globals;
	}
	
	public Object execute(String chunk) throws Throwable {
		// Tokenize and parse the chunk
		var text = new TokenizedText(lexer, transformer);
		var tokens = text.apply(chunk, 0, 0);
		var parseResult = parser.parseFully(SpecialNodes.Chunk.class, tokens);
		if (!parseResult.errors().isEmpty()) {
			throw new LuaParserException(parseResult.errors());
		}
		
		// Run semantic analysis and generate IR
		var rootScope = LuaScope.chunkRoot();
		var rootNode = parseResult.node().toIr(rootScope);
		
		// Instantiate the module
		var type = LuaType.function(
				List.of(new UpvalueTemplate((LuaLocalVar) rootScope.resolve("_ENV"), LuaType.TABLE)),
				List.of(),
				rootNode
				);
		var module = new LuaFunction(type, new Object[] {globals});
		return module.call();
	}
}
