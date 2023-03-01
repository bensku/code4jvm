package fi.benjami.parserkit.parser.internal;

import java.util.Set;

import fi.benjami.parserkit.parser.Input;
import fi.benjami.parserkit.parser.NodeRegistry;
import fi.benjami.parserkit.parser.PredictSet;

/**
 * Input that repeats zero or more times.
 *
 */
public record RepeatingInput(
		Input input
) implements Input {

	@Override
	public PredictSet predictSet(NodeRegistry nodes, Set<Input> visitedInputs) {
		// The pattern is allowed to repeat zero times
		return PredictSet.everything();
	}

}
