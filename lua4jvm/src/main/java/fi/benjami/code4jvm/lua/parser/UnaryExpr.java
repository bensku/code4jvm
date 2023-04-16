package fi.benjami.code4jvm.lua.parser;

import fi.benjami.parserkit.parser.ast.ChildNode;

public interface UnaryExpr extends Expression {

	Expression expr();
	
	public record Not(
			@ChildNode("expr") Expression expr
	) implements UnaryExpr {
		
	}
	
	public record ArrayLength(
			@ChildNode("expr") Expression expr
	) implements UnaryExpr {
		
	}
	
	public record Negate(
			@ChildNode("expr") Expression expr
	) implements UnaryExpr {
		
	}
	
	public record BitwiseNot(
			@ChildNode("expr") Expression expr
	) implements UnaryExpr {
		
	}
}
