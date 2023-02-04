package fi.benjami.parserkit.lexer.internal;

import fi.benjami.parserkit.lexer.Pattern;

public record CompoundPattern(
	boolean oneOf,
	Pattern[] patterns
) implements Pattern {}
