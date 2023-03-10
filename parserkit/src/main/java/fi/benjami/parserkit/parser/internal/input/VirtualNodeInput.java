package fi.benjami.parserkit.parser.internal.input;

import java.util.Set;

import fi.benjami.parserkit.parser.Input;
import fi.benjami.parserkit.parser.NodeRegistry;
import fi.benjami.parserkit.parser.PredictSet;

public record VirtualNodeInput(
		String inputId,
		Input input
) implements Input {

	@Override
	public PredictSet predictSet(NodeRegistry nodes, Set<Input> visitedInputs) {
		return input.predictSet(nodes, visitedInputs);
	}

}
