package fi.benjami.parserkit.lexer.internal;

import fi.benjami.parserkit.lexer.Pattern;

public record RepeatingPattern(
		Pattern pattern
) implements Pattern {}
