package fi.benjami.code4jvm.lua.parser;

import java.util.List;

import fi.benjami.code4jvm.lua.ir.IrNode;
import fi.benjami.code4jvm.lua.ir.expr.ArithmeticExpr;
import fi.benjami.code4jvm.lua.ir.expr.CompareExpr;
import fi.benjami.code4jvm.lua.ir.expr.LogicalExpr;
import fi.benjami.code4jvm.lua.ir.expr.StringConcatExpr;
import fi.benjami.code4jvm.lua.semantic.LuaScope;
import fi.benjami.parserkit.parser.Input;
import fi.benjami.parserkit.parser.ast.ChildNode;

public interface BinaryExpr extends Expression {
	
	Expression lhs();
	Expression rhs();
	
	// TODO everything except Power and StringConcat should be LEFT associative
	// this could be done by patching AST after the fact, or during code generation
	// This only matters when operators have been overloaded to have side effects
	
	public record Power(
			@ChildNode("lhs") Expression lhs,
			@ChildNode("rhs") Expression rhs
	) implements BinaryExpr {
		
		public static final Input PATTERN = Input.allOf(
				Input.virtualNode("lhs", Expression.EXPRESSIONS),
				Input.token(LuaToken.POWER),
				Input.virtualNode("rhs", Expression.EXPRESSIONS)
				);

		@Override
		public IrNode toIr(LuaScope scope) {
			return new ArithmeticExpr(lhs.toIr(scope), ArithmeticExpr.Kind.POWER, rhs.toIr(scope));
		}
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
		
		@Override
		public IrNode toIr(LuaScope scope) {
			return new ArithmeticExpr(lhs.toIr(scope), ArithmeticExpr.Kind.MULTIPLY, rhs.toIr(scope));
		}
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
		
		@Override
		public IrNode toIr(LuaScope scope) {
			return new ArithmeticExpr(lhs.toIr(scope), ArithmeticExpr.Kind.DIVIDE, rhs.toIr(scope));
		}
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
		
		@Override
		public IrNode toIr(LuaScope scope) {
			return new ArithmeticExpr(lhs.toIr(scope), ArithmeticExpr.Kind.FLOOR_DIVIDE, rhs.toIr(scope));
		}
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
		
		@Override
		public IrNode toIr(LuaScope scope) {
			return new ArithmeticExpr(lhs.toIr(scope), ArithmeticExpr.Kind.MODULO, rhs.toIr(scope));
		}
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
		
		@Override
		public IrNode toIr(LuaScope scope) {
			return new ArithmeticExpr(lhs.toIr(scope), ArithmeticExpr.Kind.ADD, rhs.toIr(scope));
		}
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
		
		@Override
		public IrNode toIr(LuaScope scope) {
			return new ArithmeticExpr(lhs.toIr(scope), ArithmeticExpr.Kind.SUBTRACT, rhs.toIr(scope));
		}
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
		
		@Override
		public IrNode toIr(LuaScope scope) {
			// TODO flatten multiple concatenations into single one to improve performance
			return new StringConcatExpr(List.of(lhs.toIr(scope), rhs.toIr(scope)));
		}
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
		
		@Override
		public IrNode toIr(LuaScope scope) {
			throw new UnsupportedOperationException();
		}
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
		
		@Override
		public IrNode toIr(LuaScope scope) {
			throw new UnsupportedOperationException();
		}
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
		
		@Override
		public IrNode toIr(LuaScope scope) {
			throw new UnsupportedOperationException();
		}
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
		
		@Override
		public IrNode toIr(LuaScope scope) {
			throw new UnsupportedOperationException();
		}
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
		
		@Override
		public IrNode toIr(LuaScope scope) {
			throw new UnsupportedOperationException();
		}
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
		
		@Override
		public IrNode toIr(LuaScope scope) {
			return new CompareExpr(lhs.toIr(scope), CompareExpr.Kind.LESS_THAN, rhs.toIr(scope));
		}
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
		
		@Override
		public IrNode toIr(LuaScope scope) {
			return new CompareExpr(lhs.toIr(scope), CompareExpr.Kind.MORE_THAN, rhs.toIr(scope));
		}
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
		
		@Override
		public IrNode toIr(LuaScope scope) {
			return new CompareExpr(lhs.toIr(scope), CompareExpr.Kind.LESS_OR_EQUAL, rhs.toIr(scope));
		}
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
		
		@Override
		public IrNode toIr(LuaScope scope) {
			return new CompareExpr(lhs.toIr(scope), CompareExpr.Kind.MORE_OR_EQUAL, rhs.toIr(scope));
		}
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
		
		@Override
		public IrNode toIr(LuaScope scope) {
			return new CompareExpr(lhs.toIr(scope), CompareExpr.Kind.NOT_EQUAL, rhs.toIr(scope));
		}
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
		
		@Override
		public IrNode toIr(LuaScope scope) {
			return new CompareExpr(lhs.toIr(scope), CompareExpr.Kind.EQUAL, rhs.toIr(scope));
		}
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
		
		@Override
		public IrNode toIr(LuaScope scope) {
			return new LogicalExpr(lhs.toIr(scope), LogicalExpr.Kind.AND, rhs.toIr(scope));
		}
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
		
		@Override
		public IrNode toIr(LuaScope scope) {
			return new LogicalExpr(lhs.toIr(scope), LogicalExpr.Kind.OR, rhs.toIr(scope));
		}
	}
}
