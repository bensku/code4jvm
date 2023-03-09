package fi.benjami.parserkit.parser;

import fi.benjami.parserkit.lexer.Token;

public class ParseError {

	private final int start, end;
	
	public ParseError(int start, int end) {
		this.start = start;
		this.end = end;
	}
	
	/**
	 * Created by parser when it encounters error tokens.
	 *
	 */
	public static class LexicalError extends ParseError {

		private final String errorToken;
		
		public LexicalError(int start, int end, String errorToken) {
			super(start, end);
			this.errorToken = errorToken;
		}
		
		
		
	}
	
	public static class UnexpectedToken extends ParseError {
		
		private final Token token;
		
		public UnexpectedToken(int start, int end, Token token) {
			super(start, end);
			this.token = token;
		}
		
	}
	
	public static class MissingToken extends ParseError {
		
		private final Token token;
		
		public MissingToken(int start, int end, Token token) {
			super(start, end);
			this.token = token;
		}
	}
	
	public static class ParseFailed extends ParseError {

		public ParseFailed(int start, int end) {
			super(start, end);
		}
		
	}
}
