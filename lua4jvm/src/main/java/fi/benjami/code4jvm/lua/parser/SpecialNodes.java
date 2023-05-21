package fi.benjami.code4jvm.lua.parser;

import java.util.List;

import fi.benjami.code4jvm.lua.ir.IrNode;
import fi.benjami.code4jvm.lua.ir.LuaBlock;
import fi.benjami.code4jvm.lua.ir.expr.FunctionDeclExpr;
import fi.benjami.code4jvm.lua.semantic.LuaScope;
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

		@Override
		public LuaBlock toIr(LuaScope scope) {
			return block.toIr(scope);
		}
	}
	
	public record Block(
			@ChildNode("statements") List<Statement> statements
	) implements LuaNode {
		
		public static final Input PATTERN = Input.repeating(Input.virtualNode("statements", Statement.STATEMENTS));
		
		@Override
		public LuaBlock toIr(LuaScope scope) {
			return new LuaBlock(statements.stream()
					.map(node -> node.toIr(scope))
					.toList());
		}
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

		@Override
		public IrNode toIr(LuaScope scope) {
			var funcScope = new LuaScope(scope, true);
			var body = block.toIr(funcScope);
			
			var args = paramNames.stream()
					.map(funcScope::declare)
					.toList();
			return new FunctionDeclExpr(funcScope.upvalues(), args, body);
		}
	}
	
}
