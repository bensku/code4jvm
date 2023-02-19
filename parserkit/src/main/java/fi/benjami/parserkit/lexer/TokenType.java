package fi.benjami.parserkit.lexer;

import java.util.function.Function;

public interface TokenType {
	
	static Function<String, String> discardText() {
		return str -> null;
	}
	
	static Function<String, String> collectText() {
		return Function.identity();
	}

	int ordinal();
		
	Function<String, ?> parser();
	
	default Token read(int startInDoc, String text) {
		return new Token(startInDoc, text.length(), ordinal(), parser().apply(text));
	}
}
