package fi.benjami.parserkit.parser.internal.input;

import java.util.List;
import java.util.Set;

import fi.benjami.parserkit.lexer.TokenType;
import fi.benjami.parserkit.parser.Input;
import fi.benjami.parserkit.parser.NodeRegistry;
import fi.benjami.parserkit.parser.PredictSet;

/**
 * Input that matches exactly one of the given patterns.
 *
 */
public record ChoiceInput(
		List<Input> choices,
		Input fallback
) implements Input {

	@Override
	public PredictSet predictSet(NodeRegistry nodes, Set<Input> visitedInputs) {
		if (visitedInputs.contains(this)) {
			return PredictSet.nothing();
		}
		visitedInputs.add(this);
		
		var predictSet = new PredictSet();
		for (var choice : choices) {
			predictSet.add(choice.predictSet(nodes, visitedInputs));
		}
		return predictSet;
	}
	
	public List<Input> filterFor(NodeRegistry nodes, TokenType first) {
		return choices.stream()
				.filter(input -> input.predictSet(nodes).has(first))
				.toList();
	}

}
