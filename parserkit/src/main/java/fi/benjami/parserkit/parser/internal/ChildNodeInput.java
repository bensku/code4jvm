package fi.benjami.parserkit.parser.internal;

import java.util.Set;

import fi.benjami.parserkit.parser.Input;
import fi.benjami.parserkit.parser.NodeRegistry;
import fi.benjami.parserkit.parser.PredictSet;
import fi.benjami.parserkit.parser.ast.AstNode;

/**
 * Input that matches a pattern, producing a child AST node. Note that if
 * the node type specified requires child nodes as constructor arguments,
 * the pattern provided MUST supply them.
 *
 */
public record ChildNodeInput(
		/**
		 * Name of the AST node constructor parameter this child node
		 * should be provided as.
		 * 
		 * @see ChildNode
		 */
		String inputId,
		
		/**
		 * Type of the child AST node.
		 */
		Class<? extends AstNode> type
) implements Input {

	// FIXME infinite recursion if patterns have recursion
	// probably easiest to eliminate it by carrying around state
	// and returning empty predict set (TODO) when recursion occurs
	
	@Override
	public PredictSet predictSet(NodeRegistry nodes, Set<Input> visitedInputs) {
		if (visitedInputs.contains(this)) {
			return PredictSet.nothing();
		}
		visitedInputs.add(this);
		
		return nodes.getPattern(type).predictSet(nodes, visitedInputs);
	}
	
}
