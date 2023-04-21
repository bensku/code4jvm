package fi.benjami.parserkit.parser.internal.input;

import java.util.Set;

import fi.benjami.parserkit.parser.Input;
import fi.benjami.parserkit.parser.NodeRegistry;
import fi.benjami.parserkit.parser.PredictSet;
import fi.benjami.parserkit.parser.ast.AstNode;

public record VirtualNodeInput(
		String inputId,
		Input input,
		Class<? extends AstNode>[] types,
		boolean errorRecovery
) implements Input {

	@Override
	public PredictSet predictSet(NodeRegistry nodes, Set<Input> visitedInputs) {
		return input.predictSet(nodes, visitedInputs);
	}

}
