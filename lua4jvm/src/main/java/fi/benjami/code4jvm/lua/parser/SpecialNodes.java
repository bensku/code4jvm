package fi.benjami.code4jvm.lua.parser;

import java.util.List;

import fi.benjami.parserkit.parser.Input;
import fi.benjami.parserkit.parser.ast.AstNode;
import fi.benjami.parserkit.parser.ast.ChildNode;
import fi.benjami.parserkit.parser.ast.TokenValue;

public class SpecialNodes {
	
	public static final List<Class<? extends AstNode>> SPECIAL_NODES = List.of(Chunk.class, Block.class, FunctionBody.class);

	public record Chunk(
			@ChildNode("block") Block block
	) implements LuaNode {
		
		public static final Input PATTERN = Input.childNode("block", Block.class);
	}
	
	public record Block(
			@ChildNode("statements") List<Statement> statements
	) implements LuaNode {
		
		public static final Input PATTERN = Input.repeating(Input.virtualNode("statements", Statement.STATEMENTS));
	}
			
	// Other
	
	public record FunctionBody(
			@TokenValue("paramNames") List<String> paramNames,
			@ChildNode("block") Block block
	) implements LuaNode {
		
		public static final Input PATTERN = Input.allOf(
				Input.token(LuaToken.GROUP_BEGIN),
				Input.list(
						Input.oneOf(
								Input.token("paramNames", LuaToken.NAME),
								Input.token("paramNames", LuaToken.NAME)
								),
						0,
						Input.token(LuaToken.LIST_SEPARATOR)
						),
				Input.token(LuaToken.GROUP_END),
				Input.childNode("block", Block.class),
				Input.token(LuaToken.END)
				);
	}
	
}
