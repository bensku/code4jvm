package fi.benjami.parserkit.minipl;

public class LexerTools {

	public static boolean isWhitespace(int ch) {
		return ch == ' ' || ch == '\n';
	}
}
