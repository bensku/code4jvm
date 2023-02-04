package fi.benjami.parserkit.lexer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import fi.benjami.parserkit.lexer.internal.TokenList;

public class TokenListTest {

	@Test
	public void simpleList() {
		var list = new TokenList();
		
		var token = new TestToken(0, 5);
		list.add(token);
		var slice = list.everything();
		assertEquals(token, slice.pop());
		
		list.truncate(5);
		assertEquals(token, list.everything().pop());
	}
	
	@Test
	public void manyTokens() {
		var list = new TokenList();
		
		// Add MANY tokens to list to check multi-array behavior
		var tokens = new Token[1000];
		for (var i = 0; i < 1000; i++) {
			var token = new TestToken(i * 5, 5);
			tokens[i] = token;
			list.add(token);
		}
		
		// Retrieve tokens from list
		var slice = list.everything();
		for (var i = 0; i < 1000; i++) {
			assertEquals(tokens[i], slice.pop());
		}
	}
	
	@Test
	public void selectReplace() {
		var list = new TokenList();
		
		var tokens = new Token[1000];
		for (var i = 0; i < 1000; i++) {
			var token = new TestToken(i * 5, 5);
			tokens[i] = token;
			list.add(token);
		}
		
		var slice = list.select(2501, 2999);
		// Is the selection start correct?
		assertEquals(tokens[500], slice.peek());
		
		// Add new tokens, replacing existing ones
		for (var i = 0; i < 100; i++) {
			var token = new TestToken(2500 + i * 6, 6);
			tokens[500 + i] = token;
			list.add(token);
		}
		list.truncate(2999); // Should do nothing
		list.updatesIndices(100);
		
		// Is the selection still correct?
		for (var i = 500; i < 1000; i++) {
			assertEquals(tokens[i], slice.pop());
		}
		
		// What if we make a new slice?
		var slice2 = list.everything();
		for (var i = 0; i < 1000; i++) {
			assertEquals(tokens[i], slice2.pop());
		}
		
		// Did the indices get updated correctly?
		for (var i = 600; i < 1000; i++) {
			assertEquals(3100 + (i - 600) * 5, tokens[i].start());
		}
	}
	
	@Test
	public void selectShrink() {
		var list = new TokenList();
		
		var tokens = new Token[1000];
		for (var i = 0; i < 1000; i++) {
			var token = new TestToken(i * 5, 5);
			tokens[i] = token;
			list.add(token);
		}
		
		var slice = list.select(2501, 3999);
		
		// Add new tokens, but less than what was selected
		for (var i = 0; i < 100; i++) {
			var token = new TestToken(2500 + i * 6, 6);
			tokens[500 + i] = token;
			list.add(token);
		}
		System.arraycopy(tokens, 800, tokens, 600, 200); // Don't leave a hole
		list.truncate(3999); // Should actually truncate
		list.updatesIndices(-900);
		
		// Is the selection still correct?
		for (var i = 500; i < 800; i++) {
			assertEquals(tokens[i], slice.pop());
		}
		
		// What if we make a new slice?
		var slice2 = list.everything();
		for (var i = 0; i < 800; i++) {
			assertEquals(tokens[i], slice2.pop());
		}
		
		// Did the indices get updated correctly?
		for (var i = 600; i < 800; i++) {
			assertEquals(3100 + (i - 600) * 5, tokens[i].start());
		}
	}
	
	@Test
	public void selectGrow() {
		var list = new TokenList();
		
		var tokens = new ArrayList<TestToken>(1900);
		for (var i = 0; i < 1000; i++) {
			var token = new TestToken(i * 5, 5);
			tokens.add(token);
			list.add(token);
		}
		
		var slice = list.select(2501, 2999);
		
		// Add new tokens, replacing existing ones
		for (var i = 0; i < 1000; i++) {
			var token = new TestToken(2500 + i * 6, 6);
			if (i < 100) {
				tokens.set(500 + i, token);
			} else {				
				tokens.add(500 + i, token);
			}
			list.add(token);
		}
		list.truncate(2999); // Should do nothing
		list.updatesIndices(5500);
		
		// Is the selection still correct?
		for (var i = 500; i < 1900; i++) {
			assertEquals(tokens.get(i), slice.pop());
		}
		
		// What if we make a new slice?
		var slice2 = list.everything();
		for (var i = 0; i < 1900; i++) {
			assertEquals(tokens.get(i), slice2.pop());
		}
		
		// Did the indices get updated correctly?
		for (var i = 1500; i < 1900; i++) {
			assertEquals(8500 + (i - 1500) * 5, tokens.get(i).start());
		}
	}
	
	@Test
	public void selectAligned() {
		var list = new TokenList();
		
		var tokens = new Token[1000];
		for (var i = 0; i < 1000; i++) {
			var token = new TestToken(i * 5, 5);
			tokens[i] = token;
			list.add(token);
		}
		
		var slice = list.select(2503, 2999); // Should be equal to 2500, 3000
		// Is the selection start correct?
		assertEquals(tokens[500], slice.peek());
		
		// Add new tokens, replacing existing ones
		for (var i = 0; i < 100; i++) {
			var token = new TestToken(2500 + i * 6, 6);
			tokens[500 + i] = token;
			list.add(token);
		}
		list.truncate(2999); // Should do nothing
		list.updatesIndices(100);
		
		// Is the selection still correct?
		for (var i = 500; i < 1000; i++) {
			assertEquals(tokens[i], slice.pop());
		}
		
		// What if we make a new slice?
		var slice2 = list.everything();
		for (var i = 0; i < 1000; i++) {
			assertEquals(tokens[i], slice2.pop());
		}
		
		// Did the indices get updated correctly?
		for (var i = 600; i < 1000; i++) {
			assertEquals(3100 + (i - 600) * 5, tokens[i].start());
		}
	}
	
	@Test
	public void selectReplaceMany() {
		var list = new TokenList();
		
		var tokens = new ArrayList<TestToken>(1000);
		for (var i = 0; i < 1000; i++) {
			var token = new TestToken(i * 5, 5);
			tokens.add(token);
			list.add(token);
		}
		
		list.select(2501, 2999);
		
		// Add new tokens, replacing existing ones
		for (var i = 0; i < 100; i++) {
			var token = new TestToken(2500 + i * 6, 6);
			tokens.set(500 + i, token);
			list.add(token);
		}
		list.truncate(2999); // Should do nothing
		list.updatesIndices(100);
		
		var slice = list.select(2301, 2799);
		
		// DO IT AGAIN!
		for (var i = 0; i < 100; i++) {
			var token = new TestToken(2300 + i * 7, 7);
			if (i < 90) {
				tokens.set(460 + i, token);				
			} else {
				tokens.add(460 + i, token);
			}
			list.add(token);
		}
		list.truncate(2799); // Should do nothing
		list.updatesIndices(200);
		
		// Is the selection correct after two updates?
		// Note: start token is partially selected
		for (var i = 460; i < 1000; i++) {
			assertEquals(tokens.get(i), slice.pop());
		}
		
		// What if we make a new slice?
		var slice2 = list.everything();
		for (var i = 0; i < 1000; i++) {
			assertEquals(tokens.get(i), slice2.pop());
		}
	}

}
