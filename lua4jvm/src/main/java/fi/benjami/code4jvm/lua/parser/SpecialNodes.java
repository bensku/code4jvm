package fi.benjami.code4jvm.lua.parser;

import java.util.List;

import fi.benjami.parserkit.parser.Input;
import fi.benjami.parserkit.parser.ast.ChildNode;
import fi.benjami.parserkit.parser.ast.TokenValue;

public class SpecialNodes {

	public record Chunk(
			@ChildNode("block") Block block
	) implements LuaNode {
		
		public static final Input PATTERN = Input.childNode("block", Block.class);
	}
	
	public record Block(
			@ChildNode("statements") List<Statement> statements,
			// TODO parse retstat separately, but add it to end of same list?
			@ChildNode("returnStmt") Statement returnStmt
	) implements LuaNode {
		
	}
			
	// Other
	
	public record FunctionBody(
			@TokenValue("paramNames") List<String> paramNames,
			@ChildNode("block") Block block
	) implements LuaNode {
		
	}
	
	public record TableField(
			@TokenValue("name") String name,
			@ChildNode("nameExpr") Expression nameExpr,
			@ChildNode("value") Expression value
	) implements LuaNode {
		
	}
	
}
