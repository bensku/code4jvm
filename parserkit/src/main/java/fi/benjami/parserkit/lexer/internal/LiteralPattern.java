package fi.benjami.parserkit.lexer.internal;

import fi.benjami.parserkit.lexer.Pattern;

public record LiteralPattern(
	String text	
) implements Pattern {}
