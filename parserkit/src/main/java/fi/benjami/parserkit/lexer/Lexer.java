package fi.benjami.parserkit.lexer;

public interface Lexer {

	class Builder {
		
	}
	
	Token getToken(LexerInput input);
	
	boolean isWhitespace(int ch);
	
	default void skipWhitespace(LexerInput input) {
		while (input.codepointsLeft() != 0 && isWhitespace(input.getCodepoint(0))) {
			input.advance(1); // TODO batch advance?
		}
	}
}
