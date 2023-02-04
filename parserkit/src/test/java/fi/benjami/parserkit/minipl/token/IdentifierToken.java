package fi.benjami.parserkit.minipl.token;

import fi.benjami.parserkit.lexer.Token;

public class IdentifierToken extends Token {

	public final String id;
	
	public IdentifierToken(int start, String id) {
		super(start, id.length());
		this.id = id;
	}

	@Override
	public String toString() {
		return "IdentifierToken [id=" + id + "]";
	}

}
