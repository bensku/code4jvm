package fi.benjami.parserkit.lexer;

import fi.benjami.parserkit.lexer.internal.TokenList;

public class TokenizedText {
	
	private final Lexer lexer;
	private final TokenTransformer transformer;
	
	private final TokenList tokens;
	private String text;
	
	public TokenizedText(Lexer lexer, TokenTransformer transformer) {
		this.lexer = lexer;
		this.transformer = transformer;
		this.tokens = new TokenList();
		this.text = "";
	}

	public View apply(String newText, int start, int end) {
		// Compute modifier to indices of tokens after end
		var indicesAfterMod = newText.length() - (end - start);
		// Update the stored text and the end index in it
		text = text.substring(0, start) + newText + text.substring(end);
		int newEnd = end + indicesAfterMod;
		
		// Go back to nearest whitespace
		// This is required to support the correct ordering of rules
		// For example, if token A=a, B=b and C=ab could otherwise cause
		// lexing to start in middle of C, leading to inconsistent results
		for (;;) {
			var ch = text.codePointAt(start);
			if (lexer.isWhitespace(ch) || start == 0) {
				break;
			}
			start -= Character.charCount(ch);
		}
		
		// Select existing tokens and overwrite (some of) them
		// Selection detects the old tokens that are after end -> (old)end
		var slice = tokens.select(start, end);
		// select() may backtrack from start if there are existing tokens
		var lexerStart = slice.hasNext() ? slice.peek().start() : start;
		var input = new LexerInput(text, lexerStart);
		// Lexer uses indices in the new text -> newEnd
		while (input.pos() <= newEnd) {
			var token = nextToken(input);
			if (token == null) {
				break; // End of file
			}
			tokens.add(token);
		}
		
		// In case the lexer produced less tokens than the selection had before
		// truncate those unnecessary tokens
		// This is not JUST an optimization, but a requirement for below overrun
		tokens.truncate(Math.max(end, input.pos()));
		
		// Lexer may overrun the end of modified text
		// Keep tokenizing until exactly same token is produced
		// If this doesn't happen at all, tokenize to end of file
		// TODO test with error recovery
		Token tokenAfter = null;
		for (;;) {
			var token = nextToken(input);
			if (token == null) {
				break; // End of file
			}
			var oldToken = tokens.replaceNext(token);
			if (oldToken == null) {
				// Reached end of the file, but there is no EOF token
				// This happens the first time we're applying anything
				tokenAfter = token;
				break;
			}
			
			// Old token doesn't exist anywhere else, safe to mutate it
			oldToken.modifyStart(indicesAfterMod);
			if (token.equals(oldToken)) {
				// Lexer produced (essentially) same token, potentially end of file
				tokenAfter = token;
				break;
			}
		}
		
		// Update indices of unmodified tokens after the selection
		tokens.updatesIndices(indicesAfterMod);
		
		// Create view for parser
		int modifiedStart = slice.peek().start();
		// End is EOF, or the start of first token after modified area
		int modifiedEnd = tokenAfter == null ? text.length() :  tokenAfter.start() - indicesAfterMod;
		return new View(modifiedStart, modifiedEnd, slice);
	}
	
	private Token nextToken(LexerInput input) {
		var token = lexer.getToken(input);
		return token == null ? null : transformer.transform(token);
	}
	
	public View viewFromStart() {
		var slice = tokens.everything();
		return new View(0, text.length(), slice);
	}
	
	public static class View {
		
		private final int start;
		private final int end;
		
		private TokenList.Slice slice;
		
		View(int start, int end, TokenList.Slice slice) {
			this.start = start;
			this.end = end;
			this.slice = slice;
		}
		
		public int start() {
			return start;
		}
		
		public int end() {
			return end;
		}
		
		public boolean hasNext() {
			return slice.hasNext();
		}
		
		public Token peek() {
			return slice.peek();
		}
		
		public Token pop() {
			return hasNext() ? slice.pop() : null;
		}
		
		public View copy() {
			return new View(start, end, slice.copy());
		}
		
		public void advance(View view) {
			slice = view.slice;
		}
	}
	
}
