package fi.benjami.parserkit.minipl.token;

import fi.benjami.parserkit.lexer.Token;

public class AssignmentToken extends Token {

	public AssignmentToken(int start) {
		super(start, 2);
	}

}
