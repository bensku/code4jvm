package fi.benjami.parserkit.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import fi.benjami.parserkit.lexer.TokenType;
import fi.benjami.parserkit.parser.ast.AstNode;
import fi.benjami.parserkit.parser.ast.ChildNode;
import fi.benjami.parserkit.parser.ast.TokenValue;
import fi.benjami.parserkit.parser.internal.input.ChildNodeInput;
import fi.benjami.parserkit.parser.internal.input.ChoiceInput;
import fi.benjami.parserkit.parser.internal.input.CompoundInput;
import fi.benjami.parserkit.parser.internal.input.WrapperInput;
import fi.benjami.parserkit.parser.internal.input.RepeatingInput;
import fi.benjami.parserkit.parser.internal.input.TokenInput;
import fi.benjami.parserkit.parser.internal.input.VirtualNodeInput;

public interface Input {
	
	/**
	 * Creates an input that matches exactly one token of given type.
	 * @param type Token type.
	 * @return Token input.
	 */
	static Input token(TokenType type) {
		return new TokenInput(null, type);
	}
	
	/**
	 * Creates an input that matches exactly one token of given type.
	 * Value of the token is provided to parent AST node constructor if it
	 * requests it.
	 * @param inputId Input id, for {@link TokenValue} constructor argument.
	 * @param type Token type.
	 * @return Token input.
	 */
	static Input token(String inputId, TokenType type) {
		return new TokenInput(inputId, type);
	}
	
	/**
	 * Creates an input that matches exactly one of the given choices. The
	 * choices are matched in order they were given to this method.
	 * @param choices Potential inputs
	 * @return Choice input.
	 */
	static Input oneOf(Input... choices) {
		return new ChoiceInput(List.of(choices), null);
	}
	
	/**
	 * Creates an input that matches ALL of the given inputs, in order they
	 * were given.
	 * @param inputs Inputs to match.
	 * @return Compound input.
	 */
	static Input allOf(Input... inputs) {
		return new CompoundInput(List.of(inputs));
	}
	
	/**
	 * Creates an input that matches another input zero or one times.
	 * @param input Non-optional input.
	 * @return Optional input.
	 */
	static Input optional(Input input) {
		return new ChoiceInput(List.of(input), new WrapperInput(null, false, 0));
	}
	
	/**
	 * Creates an input that matches another input zero or more times.
	 * @param input Non-repeating input.
	 * @return Repeating input.
	 */
	static Input repeating(Input input) {
		return new RepeatingInput(input);
	}
	
	/**
	 * Creates an input that matches the given AST node.
	 * @param inputId Input id, for {@link ChildNode} constructor argument.
	 * @param type Type of the AST node.
	 * @return AST node input.
	 */
	static Input childNode(String inputId, Class<? extends AstNode> type) {
		return new ChildNodeInput(inputId, type);
	}
	
	/**
	 * Creates an input that matches exactly one of the given AST nodes.
	 * @param inputId Input id, for {@link ChildNode} constructor argument.
	 * @param types Types of the AST nodes.
	 * @return AST node input.
	 */
	@SafeVarargs
	static Input childNode(String inputId, Class<? extends AstNode>... types) {
		if (types.length == 1) {
			return childNode(inputId, types[0]);
		} else {
			return oneOf(Arrays.stream(types).map(type -> childNode(inputId, type)).toArray(Input[]::new));
		}
	}
	
	/**
	 * Creates an input that matches exactly one of the given AST nodes.
	 * @param inputId Input id, for {@link ChildNode} constructor argument.
	 * @param types Types of the AST nodes.
	 * @return AST node input.
	 */
	@SuppressWarnings("unchecked")
	static Input childNode(String inputId, List<Class<? extends AstNode>> types) {
		return childNode(inputId, types.toArray(Class[]::new));
	}
	
	static Input virtualNode(String inputId, VirtualNode spec) {
		var choices = Arrays.stream(spec.astNodeTypes())
				.map(type -> childNode("_virtualNode", type))
				.toList();
		Input choiceInput;
		if (spec.handlesErrors()) {
			choiceInput = new ChoiceInput(choices, new WrapperInput(null, true, spec.errorType()));
		} else {
			choiceInput = new ChoiceInput(choices, null);
		}
		// TODO move choiceInput from here to e.g. VirtualNodeInput constructor
		return new VirtualNodeInput(inputId, choiceInput, spec.astNodeTypes(), spec.handlesErrors());
	}
	
	@SafeVarargs
	static Input virtualNode(String inputId, Class<? extends AstNode>... types) {
		return virtualNode(inputId, VirtualNode.of(types));
	}
	
	@SuppressWarnings("unchecked")
	static Input virtualNode(String inputId, List<Class<? extends AstNode>> types) {
		return virtualNode(inputId, types.toArray(Class[]::new));
	}
	
	/**
	 * Creates an input that tries to match the given pattern and produces an
	 * error if that fails. The pattern will still parse, allowing this to be
	 * used as an error recovery approach.
	 * @param input Input that should be parsed.
	 * @param errorType Type of error to create.
	 * @return Wrapped input.
	 */
	static Input inputOrError(Input input, int errorType) {
		if (errorType < 0) {
			throw new IllegalArgumentException("negative error types are reserved for parserkit");
		}
		return new WrapperInput(input, true, errorType);
	}
	
	static Input list(Input element, int minimumCount, Input separator) {
		var parts = new ArrayList<Input>();
		if (minimumCount > 0) {
			parts.add(element);
		} else {
			parts.add(Input.optional(element));
		}
		for (var i = 1; i < minimumCount; i++) {
			parts.add(separator);
			parts.add(element);
		}
		parts.add(Input.repeating(Input.allOf(separator, element)));
		
		return new CompoundInput(parts);
	}
	
	default PredictSet predictSet(NodeRegistry nodes) {
		// Use identity set; similar inputs might happen and that is totally ok
		// The only problem we're trying to solve here is infinite recursion!
		return predictSet(nodes, Collections.newSetFromMap(new IdentityHashMap<>()));
	}
	
	PredictSet predictSet(NodeRegistry nodes, Set<Input> visitedInputs);

}
