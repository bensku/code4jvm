package fi.benjami.parserkit.parser.internal;

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
	public PredictSet predictSet(NodeRegistry nodes) {
		// The pattern is allowed to repeat zero times
		return PredictSet.everything();
	}

}
