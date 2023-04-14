package fi.benjami.parserkit.parser;

public class InvalidGrammarException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public InvalidGrammarException(String msg) {
		super(msg);
	}
}
