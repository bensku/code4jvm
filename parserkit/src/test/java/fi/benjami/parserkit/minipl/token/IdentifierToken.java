package fi.benjami.parserkit.minipl.token;

import fi.benjami.parserkit.lexer.Token;

public class IdentifierToken extends Token {

	public final String id;
	
	public IdentifierToken(int start, String id) {
		super(start, id.length());
		this.id = id;
	}
	
	@Override
	public boolean equals(Object o) {
		return o instanceof IdentifierToken token && token.id.equals(id) && super.equals(o);
	}

	@Override
	public String toString() {
		return "IdentifierToken [start=" + start() + ", id='" + id + "']";
	}

}
