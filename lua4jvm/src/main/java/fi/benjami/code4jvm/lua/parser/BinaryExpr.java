package fi.benjami.code4jvm.lua.parser;

import fi.benjami.parserkit.parser.Input;
import fi.benjami.parserkit.parser.ast.ChildNode;

public interface BinaryExpr extends Expression {
	
	Expression lhs();
	Expression rhs();
	
	// TODO everything except Power and StringConcat should be LEFT associative
	// this could be done by patching AST after the fact, or during code generation
	
	public record Power(
			@ChildNode("lhs") Expression lhs,
			@ChildNode("rhs") Expression rhs
	) implements BinaryExpr {
		
		public static final Input PATTERN = Input.allOf(
				Input.virtualNode("lhs", Expression.EXPRESSIONS),
				Input.token(LuaToken.POWER),
				Input.virtualNode("rhs", Expression.EXPRESSIONS)
				);
	}
	
	public record Multiply(
			@ChildNode("lhs") Expression lhs,
			@ChildNode("rhs") Expression rhs
	) implements BinaryExpr {
		
		public static final Input PATTERN = Input.allOf(
				Input.virtualNode("lhs", Expression.EXPRESSIONS),
				Input.token(LuaToken.MULTIPLY),
				Input.virtualNode("rhs", Expression.EXPRESSIONS)
				);
	}

	public record Divide(
			@ChildNode("lhs") Expression lhs,
			@ChildNode("rhs") Expression rhs
	) implements BinaryExpr {
		
		public static final Input PATTERN = Input.allOf(
				Input.virtualNode("lhs", Expression.EXPRESSIONS),
				Input.token(LuaToken.DIVIDE),
				Input.virtualNode("rhs", Expression.EXPRESSIONS)
				);
	}
	
	public record FloorDivide(
			@ChildNode("lhs") Expression lhs,
			@ChildNode("rhs") Expression rhs
	) implements BinaryExpr {
		
		public static final Input PATTERN = Input.allOf(
				Input.virtualNode("lhs", Expression.EXPRESSIONS),
				Input.token(LuaToken.FLOOR_DIVIDE),
				Input.virtualNode("rhs", Expression.EXPRESSIONS)
				);
	}
	
	public record Modulo(
			@ChildNode("lhs") Expression lhs,
			@ChildNode("rhs") Expression rhs
	) implements BinaryExpr {
		
		public static final Input PATTERN = Input.allOf(
				Input.virtualNode("lhs", Expression.EXPRESSIONS),
				Input.token(LuaToken.MODULO),
				Input.virtualNode("rhs", Expression.EXPRESSIONS)
				);
	}
	
	public record Add(
			@ChildNode("lhs") Expression lhs,
			@ChildNode("rhs") Expression rhs
	) implements BinaryExpr {
		
		public static final Input PATTERN = Input.allOf(
				Input.virtualNode("lhs", Expression.EXPRESSIONS),
				Input.token(LuaToken.ADD),
				Input.virtualNode("rhs", Expression.EXPRESSIONS)
				);
	}
	
	public record Subtract(
			@ChildNode("lhs") Expression lhs,
			@ChildNode("rhs") Expression rhs
	) implements BinaryExpr {
		
		public static final Input PATTERN = Input.allOf(
				Input.virtualNode("lhs", Expression.EXPRESSIONS),
				Input.token(LuaToken.SUBTRACT_OR_NEGATE),
				Input.virtualNode("rhs", Expression.EXPRESSIONS)
				);
	}
	
	public record StringConcat(
			@ChildNode("lhs") Expression lhs,
			@ChildNode("rhs") Expression rhs
	) implements BinaryExpr {
		
		public static final Input PATTERN = Input.allOf(
				Input.virtualNode("lhs", Expression.EXPRESSIONS),
				Input.token(LuaToken.STRING_CONCAT),
				Input.virtualNode("rhs", Expression.EXPRESSIONS)
				);
	}
	
	public record BitShiftLeft(
			@ChildNode("lhs") Expression lhs,
			@ChildNode("rhs") Expression rhs
	) implements BinaryExpr {
		
		public static final Input PATTERN = Input.allOf(
				Input.virtualNode("lhs", Expression.EXPRESSIONS),
				Input.token(LuaToken.BIT_SHIFT_LEFT),
				Input.virtualNode("rhs", Expression.EXPRESSIONS)
				);
	}
	
	public record BitShiftRight(
			@ChildNode("lhs") Expression lhs,
			@ChildNode("rhs") Expression rhs
	) implements BinaryExpr {
		
		public static final Input PATTERN = Input.allOf(
				Input.virtualNode("lhs", Expression.EXPRESSIONS),
				Input.token(LuaToken.BIT_SHIFT_RIGHT),
				Input.virtualNode("rhs", Expression.EXPRESSIONS)
				);
	}
	
	public record BitwiseAnd(
			@ChildNode("lhs") Expression lhs,
			@ChildNode("rhs") Expression rhs
	) implements BinaryExpr {
		
		public static final Input PATTERN = Input.allOf(
				Input.virtualNode("lhs", Expression.EXPRESSIONS),
				Input.token(LuaToken.BITWISE_AND),
				Input.virtualNode("rhs", Expression.EXPRESSIONS)
				);
	}
	
	public record BitwiseXor(
			@ChildNode("lhs") Expression lhs,
			@ChildNode("rhs") Expression rhs
	) implements BinaryExpr {
		
		public static final Input PATTERN = Input.allOf(
				Input.virtualNode("lhs", Expression.EXPRESSIONS),
				Input.token(LuaToken.BITWISE_XOR_OR_NOT),
				Input.virtualNode("rhs", Expression.EXPRESSIONS)
				);
	}
	
	public record BitwiseOr(
			@ChildNode("lhs") Expression lhs,
			@ChildNode("rhs") Expression rhs
	) implements BinaryExpr {
		
		public static final Input PATTERN = Input.allOf(
				Input.virtualNode("lhs", Expression.EXPRESSIONS),
				Input.token(LuaToken.BITWISE_OR),
				Input.virtualNode("rhs", Expression.EXPRESSIONS)
				);
	}
	
	public record LessThan(
			@ChildNode("lhs") Expression lhs,
			@ChildNode("rhs") Expression rhs
	) implements BinaryExpr {
		
		public static final Input PATTERN = Input.allOf(
				Input.virtualNode("lhs", Expression.EXPRESSIONS),
				Input.token(LuaToken.LESS_THAN),
				Input.virtualNode("rhs", Expression.EXPRESSIONS)
				);
	}
	
	public record MoreThan(
			@ChildNode("lhs") Expression lhs,
			@ChildNode("rhs") Expression rhs
	) implements BinaryExpr {
		
		public static final Input PATTERN = Input.allOf(
				Input.virtualNode("lhs", Expression.EXPRESSIONS),
				Input.token(LuaToken.MORE_THAN),
				Input.virtualNode("rhs", Expression.EXPRESSIONS)
				);
	}
	
	public record LessOrEqual(
			@ChildNode("lhs") Expression lhs,
			@ChildNode("rhs") Expression rhs
	) implements BinaryExpr {
		
		public static final Input PATTERN = Input.allOf(
				Input.virtualNode("lhs", Expression.EXPRESSIONS),
				Input.token(LuaToken.LESS_OR_EQUAL),
				Input.virtualNode("rhs", Expression.EXPRESSIONS)
				);
	}
	
	public record MoreOrEqual(
			@ChildNode("lhs") Expression lhs,
			@ChildNode("rhs") Expression rhs
	) implements BinaryExpr {
		
		public static final Input PATTERN = Input.allOf(
				Input.virtualNode("lhs", Expression.EXPRESSIONS),
				Input.token(LuaToken.MORE_OR_EQUAL),
				Input.virtualNode("rhs", Expression.EXPRESSIONS)
				);
	}
	
	public record NotEqual(
			@ChildNode("lhs") Expression lhs,
			@ChildNode("rhs") Expression rhs
	) implements BinaryExpr {
		
		public static final Input PATTERN = Input.allOf(
				Input.virtualNode("lhs", Expression.EXPRESSIONS),
				Input.token(LuaToken.NOT_EQUAL),
				Input.virtualNode("rhs", Expression.EXPRESSIONS)
				);
	}
	
	public record Equal(
			@ChildNode("lhs") Expression lhs,
			@ChildNode("rhs") Expression rhs
	) implements BinaryExpr {
		
		public static final Input PATTERN = Input.allOf(
				Input.virtualNode("lhs", Expression.EXPRESSIONS),
				Input.token(LuaToken.EQUAL),
				Input.virtualNode("rhs", Expression.EXPRESSIONS)
				);
	}
	
	public record LogicalAnd(
			@ChildNode("lhs") Expression lhs,
			@ChildNode("rhs") Expression rhs
	) implements BinaryExpr {
		
		public static final Input PATTERN = Input.allOf(
				Input.virtualNode("lhs", Expression.EXPRESSIONS),
				Input.token(LuaToken.LOGICAL_AND),
				Input.virtualNode("rhs", Expression.EXPRESSIONS)
				);
	}
	
	public record LogicalOr(
			@ChildNode("lhs") Expression lhs,
			@ChildNode("rhs") Expression rhs
	) implements BinaryExpr {
		
		public static final Input PATTERN = Input.allOf(
				Input.virtualNode("lhs", Expression.EXPRESSIONS),
				Input.token(LuaToken.LOGICAL_OR),
				Input.virtualNode("rhs", Expression.EXPRESSIONS)
				);
	}
}
