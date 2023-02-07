package fi.benjami.parserkit.lexer;

import java.util.Arrays;

import fi.benjami.parserkit.lexer.internal.CompoundPattern;
import fi.benjami.parserkit.lexer.internal.LiteralPattern;
import fi.benjami.parserkit.lexer.internal.OptionalPattern;
import fi.benjami.parserkit.lexer.internal.RepeatingPattern;

public interface LexerPattern {

	public static LexerPattern literal(String text) {
		return new LiteralPattern(text);
	}
	
	public static LexerPattern oneOf(LexerPattern... patterns) {
		return new CompoundPattern(true, patterns);
	}
	
	public static LexerPattern of(LexerPattern... patterns) {
		return new CompoundPattern(false, patterns);
	}
	
	public static LexerPattern optional(LexerPattern pattern) {
		return new OptionalPattern(pattern);
	}
	
	public static LexerPattern repeating(LexerPattern pattern, int atLeast, int atMost) {
		if (atLeast > atMost) {
			throw new IllegalArgumentException("atLeast > atMost");
		}
		LexerPattern rest;
		if (atMost == Integer.MAX_VALUE) {
			// Unbounded repeating pattern
			rest = new RepeatingPattern(pattern);
		} else {
			// Repetitions are limited
			rest = LexerPattern.optional(atMost(pattern, atMost - atLeast));
		}
		
		if (atLeast != 0) {
			var patterns = new LexerPattern[atLeast + 1];
			Arrays.fill(patterns, pattern);
			patterns[patterns.length - 1] = rest;
			return LexerPattern.of(patterns);
		} else {
			return rest;
		}
	}
	
	private static LexerPattern atMost(LexerPattern pattern, int count) {
		if (count == 1) {
			return pattern;
		} else {			
			return LexerPattern.of(pattern, optional(atMost(pattern, count - 1)));
		}
	}

}
