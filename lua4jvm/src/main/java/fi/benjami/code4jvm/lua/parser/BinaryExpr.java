package fi.benjami.code4jvm.lua.parser;

import fi.benjami.parserkit.parser.ast.ChildNode;

public interface BinaryExpr extends Expression {
	
	Expression lhs();
	Expression rhs();
	
	public record Power(
			@ChildNode("lhs") Expression lhs,
			@ChildNode("rhs") Expression rhs
	) implements BinaryExpr {
		
	}
	
	public record Multiply(
			@ChildNode("lhs") Expression lhs,
			@ChildNode("rhs") Expression rhs
	) implements BinaryExpr {
		
	}

	public record Divide(
			@ChildNode("lhs") Expression lhs,
			@ChildNode("rhs") Expression rhs
	) implements BinaryExpr {
		
	}
	
	public record FloorDivide(
			@ChildNode("lhs") Expression lhs,
			@ChildNode("rhs") Expression rhs
	) implements BinaryExpr {
		
	}
	
	public record Modulo(
			@ChildNode("lhs") Expression lhs,
			@ChildNode("rhs") Expression rhs
	) implements BinaryExpr {
		
	}
	
	public record Add(
			@ChildNode("lhs") Expression lhs,
			@ChildNode("rhs") Expression rhs
	) implements BinaryExpr {
		
	}
	
	public record Subtract(
			@ChildNode("lhs") Expression lhs,
			@ChildNode("rhs") Expression rhs
	) implements BinaryExpr {
		
	}
	
	public record StringConcat(
			@ChildNode("lhs") Expression lhs,
			@ChildNode("rhs") Expression rhs
	) implements BinaryExpr {
		
	}
	
	public record BitShiftLeft(
			@ChildNode("lhs") Expression lhs,
			@ChildNode("rhs") Expression rhs
	) implements BinaryExpr {
		
	}
	
	public record BitShiftRight(
			@ChildNode("lhs") Expression lhs,
			@ChildNode("rhs") Expression rhs
	) implements BinaryExpr {
		
	}
	
	public record BitwiseAnd(
			@ChildNode("lhs") Expression lhs,
			@ChildNode("rhs") Expression rhs
	) implements BinaryExpr {
		
	}
	
	public record BitwiseXor(
			@ChildNode("lhs") Expression lhs,
			@ChildNode("rhs") Expression rhs
	) implements BinaryExpr {
		
	}
	
	public record BitwiseOr(
			@ChildNode("lhs") Expression lhs,
			@ChildNode("rhs") Expression rhs
	) implements BinaryExpr {
		
	}
	
	public record LessThan(
			@ChildNode("lhs") Expression lhs,
			@ChildNode("rhs") Expression rhs
	) implements BinaryExpr {
		
	}
	
	public record MoreThan(
			@ChildNode("lhs") Expression lhs,
			@ChildNode("rhs") Expression rhs
	) implements BinaryExpr {
		
	}
	
	public record LessOrEqual(
			@ChildNode("lhs") Expression lhs,
			@ChildNode("rhs") Expression rhs
	) implements BinaryExpr {
		
	}
	
	public record MoreOrEqual(
			@ChildNode("lhs") Expression lhs,
			@ChildNode("rhs") Expression rhs
	) implements BinaryExpr {
		
	}
	
	public record NotEqual(
			@ChildNode("lhs") Expression lhs,
			@ChildNode("rhs") Expression rhs
	) implements BinaryExpr {
		
	}
	
	public record Equal(
			@ChildNode("lhs") Expression lhs,
			@ChildNode("rhs") Expression rhs
	) implements BinaryExpr {
		
	}
	
	public record LogicalAnd(
			@ChildNode("lhs") Expression lhs,
			@ChildNode("rhs") Expression rhs
	) implements BinaryExpr {
		
	}
	
	public record LogicalOr(
			@ChildNode("lhs") Expression lhs,
			@ChildNode("rhs") Expression rhs
	) implements BinaryExpr {
		
	}
}
