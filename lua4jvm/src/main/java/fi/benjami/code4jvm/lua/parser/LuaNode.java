package fi.benjami.code4jvm.lua.parser;

import fi.benjami.parserkit.parser.NodeRegistry;
import fi.benjami.parserkit.parser.ast.AstNode;

public interface LuaNode extends AstNode {

	public static final NodeRegistry REGISTRY = initRegistry();
	
	private static NodeRegistry initRegistry() {
		var registry = new NodeRegistry();
		registry.register(SpecialNodes.SPECIAL_NODES);
		registry.register(Statement.STATEMENTS.astNodeTypes());
		registry.register(Statement.FunctionName.class); // Move these somewhere else
		registry.register(Statement.ElseIfClause.class);
		registry.register(Expression.EXPRESSIONS.astNodeTypes());
		
		return registry;
	}
}
