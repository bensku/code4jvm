package fi.benjami.parserkit.lexer.internal;

import fi.benjami.parserkit.lexer.Pattern;

public record OptionalPattern(
		Pattern pattern
) implements Pattern {}
