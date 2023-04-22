package fi.benjami.parserkit.lexer.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import fi.benjami.parserkit.lexer.LexerPattern;

public class LexerState {
	
	// TODO rule table
	private final Map<Integer, LexerState> transitions;
	
	private LexerState unboundedRepeat;
	private TokenCreator creator;
	
	public LexerState() {
		this.transitions = new HashMap<>();
	}
	
	public Stream<LexerState> add(LexerPattern pattern) {
		if (pattern instanceof LiteralPattern literal) {
			return Stream.of(addLiteral(literal.text(), 0));
		} else if (pattern instanceof CompoundPattern compound) {
			if (compound.oneOf()) {
				// Multiple patterns match this pattern
				return Arrays.stream(compound.patterns())
						.map(this::add)
						.flatMap(Function.identity());
			} else {
				// Patterns should be concatenated
				return addConcat(compound.patterns(), 0);
			}
		} else if (pattern instanceof OptionalPattern optional) {
			// this = pattern not present, rest = pattern is present
			var ends = Stream.concat(Stream.of(this), add(optional.pattern()));
			return ends;
		} else if (pattern instanceof RepeatingPattern repeating) {
			// Note: Pattern#repeating(...) does not always produce RepeatingPatterns
			
			// Create a sub-state that handles ONLY the unbounded repeat
			// (this is used for many repeats)
			var state = new LexerState();
			state.add(repeating.pattern());
			
			// Assign it to end states (pattern has already repeated at least once)
			var ends = add(repeating.pattern())
					.map(end -> {
						end.unboundedRepeat = state;
						return end;
					});
			// Return the states, including this one (no repeats)
			return Stream.concat(Stream.of(this), ends);
		} else {
			throw new AssertionError();
		}
	}
	
	public void setCreator(TokenCreator creator) {
		if (this.creator != null) {
			throw new IllegalArgumentException("conflicting patterns");
		}
		this.creator = creator;
	}
	
	private LexerState addLiteral(String literal, int index) {
		var ch = literal.codePointAt(index);
		var next = transitions.computeIfAbsent(ch, k -> new LexerState());
		var newIndex = index + Character.charCount(ch);
		if (newIndex != literal.length()) {
			// Continue deeper into table
			return next.addLiteral(literal, newIndex);
		} else {
			// Reached end of the literal
			return next;
		}
	}
	
	private Stream<LexerState> addConcat(LexerPattern[] patterns, int index) {
		var paths = add(patterns[index]);
		var newIndex = index + 1;
		if (newIndex != patterns.length) {
			return paths
					.map(next -> next.addConcat(patterns, newIndex))
					.flatMap(Function.identity());
		} else {
			return paths;
		}
	}

}
