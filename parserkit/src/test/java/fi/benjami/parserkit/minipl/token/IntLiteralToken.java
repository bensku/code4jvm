package fi.benjami.parserkit.minipl.token;

import fi.benjami.parserkit.lexer.Token;

public class IntLiteralToken extends Token {

	public final int value;
	
	public IntLiteralToken(int start, int length, String text) {
		super(start, length);
		this.value = Integer.parseInt(text);
	}
	
	public IntLiteralToken(int start, String text) {
		this(start, text.length(), text);
	}
	
	@Override
	public boolean equals(Object o) {
		return o instanceof IntLiteralToken token && token.value == value && super.equals(o);
	}

	@Override
	public String toString() {
		return "IntLiteralToken [start=" + start() + ", value=" + value + "]";
	}

}
