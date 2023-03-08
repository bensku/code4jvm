package fi.benjami.parserkit.parser.internal;

import java.util.HashMap;
import java.util.Map;

import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.call.CallTarget;
import fi.benjami.parserkit.parser.ast.AstNode;

public class NodeManager {
	
	private final Type parserType;
	
	private final Map<Class<? extends AstNode>, CallTarget> astParsers;
	
	public NodeManager(Type parserType) {
		this.parserType = parserType;
		this.astParsers = new HashMap<>();
	}
	
	public CallTarget astNodeParser(Class<? extends AstNode> type) {
		return astParsers.computeIfAbsent(type, k -> {
			var name = "parseNode$" + type.getSimpleName();
			// TODO > 64 nodes blacklist support
			return parserType.staticMethod(ParserGenerator.AST_NODE, name, ParserGenerator.TOKEN_VIEW, Type.LONG);
		});
	}
	
	public Map<Class<? extends AstNode>, CallTarget> astNodeParsers() {
		return astParsers;
	}
}
