package fi.benjami.parserkit.parser;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fi.benjami.parserkit.parser.ast.AstNode;

public final class NodeRegistry {

	private final Map<Class<? extends AstNode>, Input> nodePatterns;
	private final Map<Class<? extends AstNode>, Integer> nodeTypeIds;
	
	public NodeRegistry() {
		this.nodePatterns = new HashMap<>();
		this.nodeTypeIds = new HashMap<>();
	}
	
	public void register(Class<? extends AstNode> nodeType, Input pattern) {
		if (nodePatterns.containsKey(nodeType)) {
			if (!nodePatterns.get(nodeType).equals(pattern)) {				
				throw new IllegalArgumentException("node type " + nodeType + " already registered with different pattern");
			} else {
				return; // Ignore double registration with same pattern
			}
		}
		nodePatterns.put(nodeType, pattern);
		nodeTypeIds.put(nodeType, nodeTypeIds.size());
	}
	
	public void register(Class<? extends AstNode> nodeType) {
		Field field;
		try {
			field = nodeType.getDeclaredField("PATTERN");
			if (!field.getType().equals(Input.class)) {
				throw new IllegalArgumentException("PATTERN field has wrong type; expected Input, was " + field.getType());
			}
			register(nodeType, (Input) field.get(null));
		} catch (NoSuchFieldException | SecurityException e) {
			throw new IllegalArgumentException("failed to read PATTERN field of " + nodeType, e);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("PATTERN is not a static field in " + nodeType, e);
		} catch (IllegalAccessException e) {
			throw new IllegalArgumentException("failed to read PATTERN field of " + nodeType, e);
		}
	}
	
	@SafeVarargs
	public final void register(Class<? extends AstNode>... nodeTypes) {
		for (var type : nodeTypes) {
			register(type);
		}
	}
	
	public void register(List<Class<? extends AstNode>> nodeTypes) {
		for (var type : nodeTypes) {
			register(type);
		}
	}
	
	public Input getPattern(Class<? extends AstNode> nodeType) {
		var input = nodePatterns.get(nodeType);
		if (input == null) {
			throw new IllegalArgumentException("node type " + nodeType + " not registered");
		}
		return input;
	}
	
	public int getTypeId(Class<? extends AstNode> nodeType) {
		var id = nodeTypeIds.get(nodeType);
		if (id == null) {
			throw new IllegalArgumentException("node type " + nodeType + " not registered");
		}
		return id;
	}
	
	public Collection<Class<? extends AstNode>> nodeTypes() {
		return nodePatterns.keySet();
	}
	
}
