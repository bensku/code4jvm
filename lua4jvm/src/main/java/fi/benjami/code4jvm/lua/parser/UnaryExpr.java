package fi.benjami.code4jvm.lua.parser;

import fi.benjami.parserkit.parser.Input;
import fi.benjami.parserkit.parser.ast.ChildNode;

public interface UnaryExpr extends Expression {

	Expression expr();
	
	public record LogicalNot(
			@ChildNode("expr") Expression expr
	) implements UnaryExpr {
		
		public static final Input PATTERN = Input.allOf(
				Input.token(LuaToken.LOGICAL_NOT),
				Input.virtualNode("expr", Expression.EXPRESSIONS)
				);
	}
	
	public record ArrayLength(
			@ChildNode("expr") Expression expr
	) implements UnaryExpr {
		
		public static final Input PATTERN = Input.allOf(
				Input.token(LuaToken.ARRAY_LENGTH),
				Input.virtualNode("expr", Expression.EXPRESSIONS)
				);
	}
	
	public record Negate(
			@ChildNode("expr") Expression expr
	) implements UnaryExpr {
		
		public static final Input PATTERN = Input.allOf(
				Input.token(LuaToken.SUBTRACT_OR_NEGATE),
				Input.virtualNode("expr", Expression.EXPRESSIONS)
				);
	}
	
	public record BitwiseNot(
			@ChildNode("expr") Expression expr
	) implements UnaryExpr {
		
		public static final Input PATTERN = Input.allOf(
				Input.token(LuaToken.BITWISE_XOR_OR_NOT),
				Input.virtualNode("expr", Expression.EXPRESSIONS)
				);
	}
}
