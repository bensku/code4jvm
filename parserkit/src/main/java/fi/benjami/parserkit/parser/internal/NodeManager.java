package fi.benjami.parserkit.parser.internal;

import java.util.HashMap;
import java.util.Map;

import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.call.CallTarget;
import fi.benjami.parserkit.parser.Input;
import fi.benjami.parserkit.parser.ast.AstNode;
import fi.benjami.parserkit.parser.internal.input.VirtualNodeInput;

public class NodeManager {
	
	private final Type parserType;
	
	private final Map<Class<? extends AstNode>, CallTarget> astParsers;
	private final Map<Input, CallTarget> virtualParsers;
	
	public NodeManager(Type parserType) {
		this.parserType = parserType;
		this.astParsers = new HashMap<>();
		this.virtualParsers = new HashMap<>();
	}
	
	public CallTarget astNodeParser(Class<? extends AstNode> type) {
		return astParsers.computeIfAbsent(type, k -> {
			var name = "parseAst$" + type.getSimpleName();
			// TODO > 64 nodes blacklist support
			return parserType.staticMethod(ParserGenerator.AST_NODE, name, ParserGenerator.TOKEN_VIEW, Type.LONG, ParserGenerator.SET);
		});
	}
	
	public CallTarget virtualNodeParser(VirtualNodeInput input) {
		return virtualParsers.computeIfAbsent(input.input(), k -> {
			var name = "parseVirtual$" + virtualParsers.size();
			// TODO > 64 nodes blacklist support
			return parserType.staticMethod(ParserGenerator.AST_NODE, name, ParserGenerator.TOKEN_VIEW, Type.LONG, Type.INT, ParserGenerator.SET);
		});
	}
	
	public boolean hasParser(VirtualNodeInput input) {
		return virtualParsers.containsKey(input.input());
	}
	
	public Map<Class<? extends AstNode>, CallTarget> astNodeParsers() {
		return astParsers;
	}
}
