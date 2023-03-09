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
	FOR_DIVIDER(TokenType.discardText()),
	
	// Tokens that actually contain state
	IDENTIFIER(TokenType.collectText()),
	COMMENT(TokenType.collectText(), FLAG_INVISIBLE),
	INT_LITERAL(Integer::parseInt),
	STRING_LITERAL(TokenType.collectText()),
	
	// Various identifiers (created by our token transformer)
	DECLARE_VAR(TokenType.discardText()),
	FOR(TokenType.discardText()),
	FOR_IN(TokenType.discardText()),
	IF(TokenType.discardText()),
	IF_ELSE(TokenType.discardText()),
	BLOCK_DO(TokenType.discardText()),
	BLOCK_END(TokenType.discardText()),
	BUILTIN_READ(TokenType.discardText()),
	BUILTIN_PRINT(TokenType.discardText()),
	
	// The error token
	ERROR(TokenType.collectText());
	
	private final Function<String, ?> parser;
	private final int flags;
	
	MiniPlTokenType(Function<String, ?> parser, int flags) {
		this.parser = parser;
		this.flags = flags;
	}
	
	MiniPlTokenType(Function<String, ?> parser) {
		this(parser, 0);
	}

	@Override
	public Function<String, ?> parser() {
		return parser;
	}

	@Override
	public int flags() {
		return flags;
	}

}
