package fi.benjami.parserkit.parser.internal;

import java.util.Arrays;

import fi.benjami.parserkit.lexer.TokenType;
import fi.benjami.parserkit.parser.Input;
import fi.benjami.parserkit.parser.NodeRegistry;
import fi.benjami.parserkit.parser.PredictSet;

/**
 * Input that matches exactly one of the given patterns.
 *
 */
public record ChoiceInput(
		Input[] choices
) implements Input {

	@Override
	public PredictSet predictSet(NodeRegistry nodes) {
		var predictSet = new PredictSet();
		for (var choice : choices) {
			predictSet.add(choice.predictSet(nodes));
		}
		return predictSet;
	}
	
	public Input[] filterFor(NodeRegistry nodes, TokenType first) {
		return Arrays.stream(choices)
				.filter(input -> input.predictSet(nodes).has(first))
				.toArray(Input[]::new);
	}

}
