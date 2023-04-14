package fi.benjami.parserkit.lexer;

import java.util.function.Function;

public interface TokenType {
	
	static final int FLAG_INVISIBLE = 1, FLAG_ERROR = 1 << 1;
	
	static Function<String, String> discardText() {
		return str -> null;
	}
	
	static Function<String, String> collectText() {
		return Function.identity();
	}

	int ordinal();
		
	Function<String, ?> parser();
	
	int flags();
	
	default Token read(int startInDoc, String text, int length) {
		return new Token(startInDoc, length, ordinal(), parser().apply(text));
	}
	
	default Token read(int startInDoc, String text) {
		return read(startInDoc, text, text.length());
	}
	
	/**
	 * Creates a new token of this type from the given token.
	 * @param token Token to convert to this type. Not mutated.
	 * @param value Value for the new token. Since the token given as argument
	 * might have non-string value, we cannot use {@link #parser()} for this.
	 * @return A new token of this type.
	 */
	default Token convert(Token token, Object value) {
		return new Token(token.start(), token.length(), ordinal(), value);
	}
}
