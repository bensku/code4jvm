package fi.benjami.parserkit.lexer.internal;

import fi.benjami.parserkit.lexer.LexerPattern;

public record RepeatingPattern(
		LexerPattern pattern
) implements LexerPattern {}
