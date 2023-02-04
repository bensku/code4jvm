package fi.benjami.parserkit.minipl.token;

import fi.benjami.parserkit.lexer.Token;

public class ErrorToken extends Token {

	public final String value;
	
	public ErrorToken(int start, int length, String text) {
		super(start, length);
		this.value = text;
	}

}
