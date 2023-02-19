package fi.benjami.parserkit.parser;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import fi.benjami.parserkit.parser.ast.AstNode;

public class NodeRegistry {

	private final Map<Class<? extends AstNode>, Input> nodeInputs;
	
	public NodeRegistry() {
		this.nodeInputs = new HashMap<>();
	}
	
	public void register(Class<? extends AstNode> nodeType, Input rootInput) {
		if (nodeInputs.containsKey(nodeType)) {
			throw new IllegalArgumentException("node type " + nodeType + " already registered");
		}
	}
	
	public Input getRootInput(Class<? extends AstNode> nodeType) {
		var input = nodeInputs.get(nodeType);
		if (input == null) {
			throw new IllegalArgumentException("node type " + nodeType + " not registered");
		}
		return input;
	}
	
	public Collection<Class<? extends AstNode>> rootNodeTypes() {
		return nodeInputs.keySet();
	}
}
