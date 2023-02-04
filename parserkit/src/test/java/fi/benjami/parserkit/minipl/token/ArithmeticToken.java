package fi.benjami.parserkit.minipl.token;

import fi.benjami.parserkit.lexer.Token;

public class ArithmeticToken extends Token {

	public enum Op {
		ADD,
		SUBTRACT,
		MULTIPLY,
		DIVIDE,
		
		// Parentheses
		GROUP_BEGIN,
		GROUP_END
	}
	
	public final Op op;
	
	public ArithmeticToken(int start, int length, Op op) {
		super(start, length);
		this.op = op;
	}

	@Override
	public String toString() {
		return "ArithmeticToken [op=" + op + "]";
	}

}
