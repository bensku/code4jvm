package fi.benjami.parserkit.lexer.internal;

import fi.benjami.parserkit.lexer.LexerPattern;

public record LiteralPattern(
	String text	
) implements LexerPattern {}
