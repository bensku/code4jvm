package fi.benjami.parserkit.lexer.internal;

import fi.benjami.parserkit.lexer.LexerPattern;

public record OptionalPattern(
		LexerPattern pattern
) implements LexerPattern {}
