package fi.benjami.parserkit.parser;

public record CompileError(
		int type,
		int start,
		int end
) {
	
	public static final int LEXICAL = -1;
}
