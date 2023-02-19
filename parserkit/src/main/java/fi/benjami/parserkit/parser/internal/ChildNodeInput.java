package fi.benjami.parserkit.parser.internal;

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

	@Override
	public PredictSet predictSet(NodeRegistry nodes) {
		return nodes.getRootInput(type).predictSet(nodes);
	}
	
}
