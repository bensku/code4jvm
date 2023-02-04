package fi.benjami.parserkit.minipl.token;

import fi.benjami.parserkit.lexer.Token;

public class LogicalOpToken extends Token {

	public enum Type {
		AND,
		NOT,
		EQUAL,
		LESS_THAN
	}
	
	public final Type type;
	
	public LogicalOpToken(int start, int length, Type type) {
		super(start, length);
		this.type = type;
	}

}
