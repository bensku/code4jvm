package fi.benjami.parserkit.lexer;

public class TestToken extends Token {
	
	public TestToken(int start, int length) {
		super(start, length);
	}
	
	@Override
	public String toString() {
		return "Token{start=" + start() + ", length=" + length() + "}";
	}
}
