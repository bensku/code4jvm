package fi.benjami.parserkit.parser;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;

import fi.benjami.parserkit.lexer.TokenType;
import fi.benjami.parserkit.lexer.TokenizedText;
import fi.benjami.parserkit.parser.ast.AstNode;
import fi.benjami.parserkit.parser.internal.ParserGenerator;

public interface Parser {
		
	static Parser compileAndLoad(NodeRegistry registry, TokenType[] tokenTypes, ParserFlag... flags) {
		var code = compile("fi.benjami.parserkit.parser.ParserImpl", registry, tokenTypes, flags);
		
		var lookup = MethodHandles.lookup();
		try {			
			var hiddenLookup = lookup.defineHiddenClass(code, true);
			var constructor = hiddenLookup.findConstructor(hiddenLookup.lookupClass(),
					MethodType.methodType(void.class));
			return (Parser) constructor.invoke();
		} catch (Throwable e) {
			// Since we just generated the class, any errors loading it or
			// calling the constructor are bugs in parserkit code
			throw new AssertionError(e);
		}
	}
	
	static byte[] compile(String className, NodeRegistry registry, TokenType[] tokenTypes, ParserFlag... flags) {
		var hookSupport = List.of(flags).contains(ParserFlag.INTERNAL_HOOK_SUPPORT);
		
		var generator = new ParserGenerator(className, registry, tokenTypes, hookSupport);
		for (var root : registry.nodeTypes()) {
			generator.addRoot(root);
		}
		return generator.compile();
	}
	
	AstNode parse(Class<? extends AstNode> nodeType, TokenizedText.View view);
}
