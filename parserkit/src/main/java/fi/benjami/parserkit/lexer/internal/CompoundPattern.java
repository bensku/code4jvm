package fi.benjami.parserkit.lexer.internal;

import fi.benjami.parserkit.lexer.LexerPattern;

public record CompoundPattern(
	boolean oneOf,
	LexerPattern[] patterns
) implements LexerPattern {}
