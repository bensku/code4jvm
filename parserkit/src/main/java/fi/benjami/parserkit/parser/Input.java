package fi.benjami.parserkit.parser;

import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import fi.benjami.parserkit.lexer.TokenType;
import fi.benjami.parserkit.parser.internal.CompoundInput;
import fi.benjami.parserkit.parser.ast.AstNode;
import fi.benjami.parserkit.parser.internal.ChildNodeInput;
import fi.benjami.parserkit.parser.internal.ChoiceInput;
import fi.benjami.parserkit.parser.internal.RepeatingInput;
import fi.benjami.parserkit.parser.internal.TokenInput;

public interface Input {
	
	static Input token(TokenType type) {
		return new TokenInput(null, type);
	}
	
	static Input token(String inputId, TokenType type) {
		return new TokenInput(inputId, type);
	}
	
	static Input oneOf(Input... choices) {
		return new ChoiceInput(List.of(choices));
	}
	
	static Input allOf(Input... inputs) {
		return new CompoundInput(List.of(inputs));
	}
	
	static Input optional(Input pattern) {
		return oneOf(pattern, allOf());
	}
	
	static Input repeating(Input input) {
		return new RepeatingInput(input);
	}
	
	static Input childNode(String inputId, Class<? extends AstNode> type) {
		return new ChildNodeInput(inputId, type);
	}
	
	@SafeVarargs
	static Input childNode(String inputId, Class<? extends AstNode>... types) {
		if (types.length == 1) {
			return childNode(inputId, types[0]);
		} else {
			return oneOf(Arrays.stream(types).map(type -> childNode(inputId, type)).toArray(Input[]::new));
		}
	}
	
	@SuppressWarnings("unchecked")
	static Input childNode(String inputId, List<Class<? extends AstNode>> types) {
		return childNode(inputId, types.toArray(Class[]::new));
	}
	
	default PredictSet predictSet(NodeRegistry nodes) {
		// Use identity set; similar inputs might happen and that is totally ok
		// The only problem we're trying to solve here is infinite recursion!
		return predictSet(nodes, Collections.newSetFromMap(new IdentityHashMap<>()));
	}
	
	PredictSet predictSet(NodeRegistry nodes, Set<Input> visitedInputs);

}
