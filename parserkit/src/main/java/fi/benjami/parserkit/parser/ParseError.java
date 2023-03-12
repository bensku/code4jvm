package fi.benjami.parserkit.parser;

public record ParseError(
		int type,
		int start,
		int end
) {
	
	public static final int LEXICAL = -1;
	public static final int NOT_FULLY_PARSED = -2;
}
