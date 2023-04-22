package fi.benjami.parserkit.parser.internal.input;

import java.util.List;
import java.util.Set;

import fi.benjami.parserkit.parser.Input;
import fi.benjami.parserkit.parser.NodeRegistry;
import fi.benjami.parserkit.parser.PredictSet;

public record CompoundInput(
		List<Input> parts
) implements Input {

	@Override
	public PredictSet predictSet(NodeRegistry nodes, Set<Input> visitedInputs) {
		if (visitedInputs.contains(this)) {
			return PredictSet.nothing();
		}
		visitedInputs.add(this);
		
		return parts.isEmpty() ? PredictSet.everything()
				: parts.get(0).predictSet(nodes, visitedInputs);
	}

}