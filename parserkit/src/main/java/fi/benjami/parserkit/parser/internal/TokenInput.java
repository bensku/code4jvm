package fi.benjami.parserkit.parser.internal;

import java.util.Set;

import fi.benjami.parserkit.lexer.TokenType;
import fi.benjami.parserkit.parser.Input;
import fi.benjami.parserkit.parser.NodeRegistry;
import fi.benjami.parserkit.parser.PredictSet;

/**
 * Input that accepts one token of given type.
 *
 */
public record TokenInput(
		/**
		 * @see TokenValue
		 */
		String inputId,
		
		/**
		 * Type of the token accepted.
		 */
		TokenType type
) implements Input {

	@Override
	public PredictSet predictSet(NodeRegistry nodes, Set<Input> visitedInputs) {
		return PredictSet.of(type);
	}

}
