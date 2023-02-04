package fi.benjami.parserkit.minipl.token;

import fi.benjami.parserkit.lexer.Token;

public class CommentToken extends Token {

	public final String text;
	
	public CommentToken(int start, int length, String text) {
		super(start, length);
		this.text = text;
	}

}
