package fi.benjami.parserkit.parser.internal;

import fi.benjami.parserkit.parser.Input;
import fi.benjami.parserkit.parser.NodeRegistry;
import fi.benjami.parserkit.parser.PredictSet;

public record CompoundInput(
		Input[] parts
) implements Input {

	@Override
	public PredictSet predictSet(NodeRegistry nodes) {
		return parts.length != 0 ? parts[0].predictSet(nodes) : PredictSet.everything();
	}

}