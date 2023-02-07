package fi.benjami.parserkit.minipl.token;

import fi.benjami.parserkit.lexer.Token;

public class StringLiteralToken extends Token {

	public final String value;
	
	public StringLiteralToken(int start, int length, String text) {
		super(start, length);
		this.value = text.substring(1, text.length() - 2); // Strip quotation marks
	}
	
	@Override
	public boolean equals(Object o) {
		return o instanceof StringLiteralToken token && token.value.equals(value) && super.equals(o);
	}

}
