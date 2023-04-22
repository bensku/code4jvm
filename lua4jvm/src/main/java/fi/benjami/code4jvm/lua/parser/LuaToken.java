package fi.benjami.code4jvm.lua.parser;

import java.util.function.Function;

import fi.benjami.parserkit.lexer.TokenType;

public enum LuaToken implements TokenType {
	// Identifiers and constants
	NAME(TokenType.collectText()), // Name
	LITERAL_NUMBER(Double::parseDouble), // FIXME hexadecimal support, see Integer::decode
	STRING_LITERAL(TokenType.collectText()),
	
	// Reserved keywords
	// (created from IDENTIFIERs during post-processing)
	LOOP_BREAK(TokenType.discardText()), // break
	GOTO_JUMP(TokenType.discardText()), // goto
	DO(TokenType.discardText()), // do
	END(TokenType.discardText()), // end
	WHILE_LOOP(TokenType.discardText()), // while
	REPEAT_LOOP(TokenType.discardText()), // repeat
	REPEAT_UNTIL(TokenType.discardText()), // until
	IF_BLOCK(TokenType.discardText()), // if
	IF_THEN(TokenType.discardText()), // then
	IF_ELSE_IF(TokenType.discardText()), // elseif
	IF_ELSE(TokenType.discardText()), // else
	FOR_LOOP(TokenType.discardText()), // for
	FOR_IN(TokenType.discardText()), // in
	FUNCTION(TokenType.discardText()), // function
	LOCAL(TokenType.discardText()), // local
	RETURN(TokenType.discardText()), // return
	LOGICAL_AND(TokenType.discardText()), // and
	LOGICAL_OR(TokenType.discardText()), // or
	LOGICAL_NOT(TokenType.discardText()), // not
	LITERAL_NIL(s -> null), // nil
	LITERAL_FALSE(s -> false), // false
	LITERAL_TRUE(s -> true), // true
	
	// Random tokens that are not keywords
	// (created directly by the lexer)
	GROUP_BEGIN(TokenType.discardText()), // (
	GROUP_END(TokenType.discardText()), // )
	TABLE_INDEX_BEGIN(TokenType.discardText()), // [
	TABLE_INDEX_END(TokenType.discardText()), // ]
	STATEMENT_END(TokenType.discardText()), // ;
	GOTO_LABEL(TokenType.discardText()), // ::
	ASSIGNMENT(TokenType.discardText()), // =
	NAME_SEPARATOR(TokenType.discardText()), // .
	LIST_SEPARATOR(TokenType.discardText()), // ,
	OOP_FUNC_SEPARATOR(TokenType.discardText()), // :
	VARARGS(TokenType.discardText()), // ...
	
	// Unary/binary operators that are not keywords
	ADD(TokenType.discardText()), // +
	SUBTRACT_OR_NEGATE(TokenType.discardText()), // -
	MULTIPLY(TokenType.discardText()), // *
	DIVIDE(TokenType.discardText()), // /
	FLOOR_DIVIDE(TokenType.discardText()), // //
	POWER(TokenType.discardText()), // ^
	MODULO(TokenType.discardText()), // %
	BITWISE_AND(TokenType.discardText()), // &
	BITWISE_XOR_OR_NOT(TokenType.discardText()), // ~
	BITWISE_OR(TokenType.discardText()), // |
	BIT_SHIFT_RIGHT(TokenType.discardText()), // >>
	BIT_SHIFT_LEFT(TokenType.discardText()), // <<
	STRING_CONCAT(TokenType.discardText()), // ..
	LESS_THAN(TokenType.discardText()), // <
	LESS_OR_EQUAL(TokenType.discardText()), // <=
	MORE_THAN(TokenType.discardText()), // >
	MORE_OR_EQUAL(TokenType.discardText()), // >=
	EQUAL(TokenType.discardText()), // ==
	NOT_EQUAL(TokenType.discardText()), // ~=
	ARRAY_LENGTH(TokenType.discardText()), // #
	
	ERROR(TokenType.collectText());
	
	// TODO variable attributes?
	;
	
	private final Function<String, ?> parser;
	private final int flags;
	
	LuaToken(Function<String, ?> parser, int flags) {
		this.parser = parser;
		this.flags = flags;
	}
	
	LuaToken(Function<String, ?> parser) {
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
