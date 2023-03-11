package fi.benjami.parserkit.parser;

public record CompileError(
		int type,
		int start,
		int end
) {
	
	public static final int LEXICAL = -1;
	public static final int NOT_FULLY_PARSED = -1;
}
