package fi.benjami.parserkit.lexer;

import java.util.Arrays;

import fi.benjami.parserkit.lexer.internal.CompoundPattern;
import fi.benjami.parserkit.lexer.internal.LiteralPattern;
import fi.benjami.parserkit.lexer.internal.OptionalPattern;
import fi.benjami.parserkit.lexer.internal.RepeatingPattern;

public interface Pattern {

	public static Pattern literal(String text) {
		return new LiteralPattern(text);
	}
	
	public static Pattern oneOf(Pattern... patterns) {
		return new CompoundPattern(true, patterns);
	}
	
	public static Pattern of(Pattern... patterns) {
		return new CompoundPattern(false, patterns);
	}
	
	public static Pattern optional(Pattern pattern) {
		return new OptionalPattern(pattern);
	}
	
	public static Pattern repeating(Pattern pattern, int atLeast, int atMost) {
		if (atLeast > atMost) {
			throw new IllegalArgumentException("atLeast > atMost");
		}
		Pattern rest;
		if (atMost == Integer.MAX_VALUE) {
			// Unbounded repeating pattern
			rest = new RepeatingPattern(pattern);
		} else {
			// Repetitions are limited
			rest = Pattern.optional(atMost(pattern, atMost - atLeast));
		}
		
		if (atLeast != 0) {
			var patterns = new Pattern[atLeast + 1];
			Arrays.fill(patterns, pattern);
			patterns[patterns.length - 1] = rest;
			return Pattern.of(patterns);
		} else {
			return rest;
		}
	}
	
	private static Pattern atMost(Pattern pattern, int count) {
		if (count == 1) {
			return pattern;
		} else {			
			return Pattern.of(pattern, optional(atMost(pattern, count - 1)));
		}
	}

}
