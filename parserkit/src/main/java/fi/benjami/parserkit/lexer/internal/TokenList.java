package fi.benjami.parserkit.lexer.internal;

import fi.benjami.parserkit.lexer.Token;

public class TokenList {
	
	private static final int SUBLIST_SIZE = 64;

	private static class Sublist {
		Token[] tokens;
		int lastIndex;
		
		Sublist next;
		
		int charIndexMod;
		
		Sublist() {
			this.tokens = new Token[SUBLIST_SIZE];
			this.lastIndex = -1;
		}
		
		void updateIndices() {
			var mod = charIndexMod;
			if (mod != 0) {
				for (int i = lastIndex; i >= 0; i--) {
					var token = tokens[i];
					if (token != null) {						
						tokens[i].modifyStart(mod);
					} // else: there is a hole, nothing to do
				}
				charIndexMod = 0;
			}
		}
		
		int start(int index) {
			return tokens[index].start() + charIndexMod;
		}
		
		int end(int index) {
			return tokens[index].end() + charIndexMod;
		}
	}
	
	public static class Slice {
		
		private Sublist list;
		private int tokenIndex;
		
		Slice(Sublist list, int tokenIndex) {
			this.list = list;
			this.tokenIndex = tokenIndex;
		}
		
		public boolean hasNext() {
			return list != null && tokenIndex <= list.lastIndex;
		}
		
		public Token peek() {
			list.updateIndices(); // Throws if there is nothing more
			var next = list.tokens[tokenIndex];
			if (next == null) {
				return findNext();
			}
			return next;
		}
		
		private Token findNext() {
			var index = tokenIndex;
			for (;;) {
				if (index > list.lastIndex) {
					if (list.next == null) {
						throw new IllegalStateException("no more tokens");
					}
					// Go to next list
					list = list.next;
					index = 0;
				}
				var token = list.tokens[index];
				if (token == null) {
					index++;
				} else {
					tokenIndex = index;
					return token;
				}
			}
			
		}
		
		public Token pop() {
			var token = peek();
			tokenIndex++;
			
			// Advance to next list if needed
			if (tokenIndex > list.lastIndex) {
				list = list.next;
				while (list != null && list.lastIndex == -1) {
					list = list.next; // Skip empty lists
				}
				tokenIndex = 0;
			}
			return token;
		}
		
		public Slice copy() {
			return new Slice(list, tokenIndex);
		}
		
	}
	
	private final Sublist first;
	private Sublist last;
	
	private Sublist current;
	private int currentToken;
	
	/**
	 * Expected selection end. {@link #add(Token)} truncates existing tokens
	 * before this is reached.
	 */
	private int expectedEnd;
			
	public TokenList() {
		this.first = new Sublist();
		this.last = first;
		select(0, Integer.MAX_VALUE); // Initially select everything
	}
	
	public Slice everything() {
		return new Slice(first, 0);
	}
	
	/**
	 * Selects the tokens between given character indices, including tokens
	 * that are partially between these indices.
	 * @param start Start character index (inclusive).
	 * @param end End character index (exclusive).
	 * @return Slice from the first token in selected area to list end.
	 */
	public Slice select(int start, int end) {
		if (first.lastIndex == -1) {
			// This token list is empty
			current = first;
			currentToken = 0;
			return everything(); // i.e. nothing
		}
		
		expectedEnd = end;
		if (start > last.end(last.lastIndex)) {
			// Selection starts after last token -> just append to end
			current = last;
			currentToken = last.lastIndex;
			return new Slice(last, last.lastIndex + 1);
		}
		
		// Seek the sublist where to begin
		var list = first;
		while (list.end(list.lastIndex) < start) {
			list = list.next;
		}
		
		// Seek the start token inside sublist
		// Loop won't need condition, it'll return once token is found
		list.updateIndices(); // We'll return one of the tokens
		for (var i = 0;; i++) {
			// If there is nothing between the start and end of the token,
			// that is the first matching token
			// This is done to support maximal munch
			if (list.tokens[i].end() >= start) {
				current = list;
				currentToken = i;
				return new Slice(list, i);
			}
		}
	}
	
	/**
	 * Adds a new token to the current selection.
	 * @param token Token to add.
	 */
	public void add(Token token) {
		if (currentToken == SUBLIST_SIZE) {
			nextSublist();
		}
		// Newly added tokens have charIndexMod == 0; existing tokens must have it too
		current.updateIndices();
		if (currentToken > current.lastIndex) {
			// Nothing important here, just append the token
			// truncate() will clear rest of the old selection if needed
			current.lastIndex++;
		} else {
			var old = current.tokens[currentToken];
			if (old != null && old.start() > expectedEnd) {
				// The previous token is after selection, don't replace it!
				// To support maximal munch, "touching" the end also counts
				splice();
			} // else: the previous token is part of selection, OK to replace it
		}
		
		current.tokens[currentToken] = token;
		currentToken++;
	}
	
	public Token replaceNext(Token token) {
		for (;;) {
			for (var i = currentToken; i <= current.lastIndex; i++) {
				var oldToken = current.tokens[i];
				if (oldToken != null) {
					current.tokens[i] = token;
					currentToken = i + 1;
					return oldToken;
				}
			}
			
			// If list is empty, remove it
			if (currentToken == 0) {
				// TODO
			}
			
			if (current.next == null) {
				// TODO should this just add the token?
				return null; // Reached the end
			}
			current = current.next;
			currentToken = 0;
		}
	}
	
	private void nextSublist() {
		var next = current.next;
		if (next == null) {
			next = new Sublist();
			current.next = next;
			last = next;
		}
		current = next;
		currentToken = 0;
	}
	
	private void splice() {
		// Copy rest of current list to end of new list
		// (at beginning, there will be nulls that mark free space)
		var newList = new Sublist();
		var count = current.lastIndex - currentToken + 1;
		System.arraycopy(current.tokens, currentToken, newList.tokens, SUBLIST_SIZE - count, count);
		newList.lastIndex = SUBLIST_SIZE - 1; // Tokens were copied to end

		// Update linked list references
		newList.next = current.next;
		current.next = newList;
		
		// Mark rest of current list as free without actually writing nulls
		current.lastIndex = currentToken;
	}
	
	/**
	 * Updates character indices of all tokens AFTER this selection.
	 * @param modifier Positive or negative modifier.
	 * 
	 * @implNote Fakes mutations wherever possible.
	 */
	public void updatesIndices(int modifier) {
		// Mutate rest of tokens in current list
		for (int i = current.lastIndex; i >= currentToken; i--) {
			current.tokens[i].modifyStart(modifier);
		}
		
		// Only mark rest of the lists
		for (var list = current.next; list != null; list = list.next) {
			list.charIndexMod += modifier; // There could be an existing modifier
		}
	}
	
	/**
	 * Truncates the old content between start of the selection and the given
	 * end index.
	 * @param end End offset. Tokens that end at or before this should be
	 * truncated.
	 * 
	 * @implNote Truncation is done only if {@link #add(Token)} didn't yet
	 * do it.
	 */
	public void truncate(int end) {
		assert end >= expectedEnd;
		var list = current;
		var lastIndex = list.lastIndex;
		if (list.end(lastIndex) <= end) {
			// Truncate rest of current list, and potentially more lists
			// TODO remove list if currentToken == 0
			list.lastIndex = currentToken - 1;
			
			// Truncate entire lists by removing them (if needed)
			while (list.end(list.lastIndex) <= end) {
				list = list.next;
				current.next = list; // Truncate entire sublist by removing the reference to it
				if (list == null) {
					return; // Previous list was last
				}
			}
			
			// Truncate part of the last list
			current = list;
			currentToken = 0;
			for (;list.start(currentToken) <= end; currentToken++) {
				list.tokens[currentToken] = null;
			}
		} else {
			// Potentially truncate part of the current list by writing nulls
			for (;currentToken <= lastIndex && list.start(currentToken) <= end; currentToken++) {
				list.tokens[currentToken] = null;
			}
		}
	}
	
}
