package fi.benjami.parserkit.minipl;

import java.util.function.Function;

import fi.benjami.parserkit.lexer.TokenType;

public enum MiniPlTokenType implements TokenType {

	// Basic arithmetic
	ADD(TokenType.discardText()),
	SUBTRACT(TokenType.discardText()),
	MULTIPLY(TokenType.discardText()),
	DIVIDE(TokenType.discardText()),
	
	// Parentheses
	GROUP_BEGIN(TokenType.discardText()),
	GROUP_END(TokenType.discardText()),
	
	// Logical operations
	LOGICAL_AND(TokenType.discardText()),
	LOGICAL_NOT(TokenType.discardText()),
	EQUALS(TokenType.discardText()),
	LESS_THAN(TokenType.discardText()),
	
	// Various markers
	VAR_TYPE(TokenType.discardText()),
	ASSIGNMENT(TokenType.discardText()),
	STATEMENT_END(TokenType.discardText()),
	
	// Tokens that actually contain state
	IDENTIFIER(TokenType.collectText()),
	COMMENT(TokenType.collectText()),
	INT_LITERAL(Integer::parseInt),
	STRING_LITERAL(TokenType.collectText()),
	
	// The error token
	ERROR(TokenType.collectText());
	
	private final Function<String, ?> parser;
	
	MiniPlTokenType(Function<String, ?> parser) {
		this.parser = parser;
	}

	@Override
	public Function<String, ?> parser() {
		return parser;
	}

}
