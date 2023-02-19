package fi.benjami.parserkit.parser;

import fi.benjami.parserkit.lexer.TokenType;
import fi.benjami.parserkit.parser.internal.CompoundInput;
import fi.benjami.parserkit.parser.ast.AstNode;
import fi.benjami.parserkit.parser.internal.ChildNodeInput;
import fi.benjami.parserkit.parser.internal.ChoiceInput;
import fi.benjami.parserkit.parser.internal.RepeatingInput;
import fi.benjami.parserkit.parser.internal.TokenInput;

public interface Input {
	
	static Input token(String inputId, TokenType type) {
		return new TokenInput(inputId, type);
	}
	
	static Input oneOf(Input... choices) {
		return new ChoiceInput(choices);
	}
	
	static Input allOf(Input... inputs) {
		return new CompoundInput(inputs);
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
	
	PredictSet predictSet(NodeRegistry nodes);

}
