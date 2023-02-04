package fi.benjami.parserkit.lexer;

import fi.benjami.parserkit.lexer.internal.TokenList;

public class TokenizedText {
	
	private final Lexer lexer;
	
	private final TokenList tokens;
	private String text;
	
	public TokenizedText(Lexer lexer) {
		this.lexer = lexer;
		this.tokens = new TokenList();
		this.text = "";
	}

	public View apply(String newText, int start, int end) {
		// Compute modifier to indices of tokens after end
		var indicesAfterMod = newText.length() - (end - start);
		// Update the stored text and the end index in it
		text = text.substring(0, start) + newText + text.substring(end);
		int newEnd = end + indicesAfterMod;
		
		// Set start to end of previous token to include it in selection
		// This should avoid violations of maximal munch
		// In contrast, the end passed to select(...) is just a guess
		int tokenStart = start;
		for (;;) {
			var ch = text.codePointAt(tokenStart);
			if (lexer.isWhitespace(tokenStart) || tokenStart == 0) {
				break;
			}
			tokenStart -= Character.charCount(ch);
		}
		if (tokenStart != 0) {
			tokenStart--;
		}
		
		// Select existing tokens and overwrite (some of) them
		// Selection detects the old tokens that are after end -> (old)end
		var slice = tokens.select(tokenStart, end);
		var input = new LexerInput(text, start);
		// Lexer uses indices in the new text -> newEnd
		while (input.pos() < newEnd) {
			tokens.add(lexer.getToken(input));
		}
		
		// In case the lexer produced less tokens than the selection had before
		// truncate those unnecessary tokens
		// This is not JUST an optimization, but a requirement for below overrun
		tokens.truncate(Math.max(end, input.pos()));
		
		// Lexer may overrun the end of modified text
		// Keep tokenizing until exactly same token is produced
		// If this doesn't happen at all, tokenize to end of file
		// TODO test with error recovery
		Token tokenAfter;
		for (;;) {
			var token = lexer.getToken(input);
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
		int modifiedEnd = tokenAfter.start() - indicesAfterMod;
		return new View(modifiedStart, modifiedEnd, slice);
	}
	
	public View viewFromStart() {
		var slice = tokens.everything();
		return new View(0, text.length(), slice);
	}
	
	public static class View {
		
		private final int start;
		private final int end;
		
		private final TokenList.Slice slice;
		
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
			return slice.pop();
		}
	}
	
}
