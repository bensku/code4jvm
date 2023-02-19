package fi.benjami.parserkit.parser;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import fi.benjami.parserkit.lexer.TokenType;
import fi.benjami.parserkit.lexer.TokenizedText;
import fi.benjami.parserkit.parser.ast.AstNode;
import fi.benjami.parserkit.parser.internal.ParserGenerator;

public interface Parser {
		
	static Parser compileAndLoad(NodeRegistry registry, TokenType[] tokenTypes) {
		var code = compile("fi.benjami.parserkit.parser.ParserImpl", registry, tokenTypes);
		
		var lookup = MethodHandles.lookup();
		try {			
			var hiddenLookup = lookup.defineHiddenClass(code, true);
			var constructor = hiddenLookup.findConstructor(hiddenLookup.lookupClass(),
					MethodType.methodType(void.class));
			return (Parser) constructor.invokeExact();
		} catch (Throwable e) {
			// Since we just generated the class, any errors loading it or
			// calling the constructor are bugs in parserkit code
			throw new AssertionError(e);
		}
	}
	
	static byte[] compile(String className, NodeRegistry registry, TokenType[] tokenTypes) {
		var generator = new ParserGenerator(className, registry, tokenTypes);
		for (var root : registry.rootNodeTypes()) {
			generator.addRoot(root);
		}
		return generator.compile();
	}
	
	AstNode parse(Class<? extends AstNode> nodeType, TokenizedText.View view);
}
