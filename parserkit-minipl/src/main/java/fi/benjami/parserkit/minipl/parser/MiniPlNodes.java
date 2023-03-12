package fi.benjami.parserkit.minipl.parser;

import java.util.List;

import fi.benjami.parserkit.parser.Input;
import fi.benjami.parserkit.parser.NodeRegistry;
import fi.benjami.parserkit.parser.VirtualNode;
import fi.benjami.parserkit.parser.ast.AstNode;
import fi.benjami.parserkit.parser.ast.ChildNode;
import fi.benjami.parserkit.parser.ast.TokenValue;

public class MiniPlNodes {
	
	public static final NodeRegistry REGISTRY = new NodeRegistry();
	
	public interface Node extends AstNode {
		void visit(MiniPlVisitor visitor);
	}
	
	public record Program(
			@ChildNode("block") Block block
	) implements Node {
		
		public static final Input PATTERN = Input.childNode("block", Block.class);
		
		@Override
		public void visit(MiniPlVisitor visitor) {
			block.visit(visitor);
			visitor.visit(this);
		}
	}
	
	public record Block(
			@ChildNode("stmt") List<Statement> statements
	) implements Node {
		
		public static final Input PATTERN = Input.repeating(Input.childNode("stmt", STATEMENTS));
		
		@Override
		public void visit(MiniPlVisitor visitor) {
			for (var stmt : statements) {
				stmt.visit(visitor);
			}
			visitor.visit(this);
		}
	}

	// Statements
	public interface Statement extends Node {}
	
	public static final List<Class<? extends AstNode>> STATEMENTS = List.of(VarDeclaration.class,
			VarAssignment.class, BuiltinRead.class, BuiltinPrint.class, IfBlock.class, ForBlock.class);
	
	public static final Input STATEMENT_END = Input.inputOrError(Input.token(MiniPlTokenType.STATEMENT_END), MiniPlError.MISSING_SEMICOLON);
	
	public record VarDeclaration(
			@TokenValue("name") String name,
			@TokenValue("type") String type,
			@ChildNode("expr") Expression initialValue
	) implements Statement {
		
		public static final Input PATTERN = Input.allOf(
				Input.token(MiniPlTokenType.DECLARE_VAR),
				Input.token("name", MiniPlTokenType.IDENTIFIER),
				Input.token(MiniPlTokenType.VAR_TYPE),
				Input.token("type", MiniPlTokenType.IDENTIFIER),
				Input.optional(Input.allOf(
						Input.token(MiniPlTokenType.ASSIGNMENT),
						Input.virtualNode("expr", EXPRESSIONS)
						)),
				STATEMENT_END
				);

		@Override
		public void visit(MiniPlVisitor visitor) {
			if (initialValue != null) {
				initialValue.visit(visitor);
			}
			visitor.visit(this);
		}
	}
	
	public record VarAssignment(
			@TokenValue("name") String name,
			@ChildNode("expr") Expression value
	) implements Statement {
		
		public static final Input PATTERN = Input.allOf(
				Input.token("name", MiniPlTokenType.IDENTIFIER),
				Input.token(MiniPlTokenType.ASSIGNMENT),
				Input.virtualNode("expr", EXPRESSIONS),
				STATEMENT_END
				);

		@Override
		public void visit(MiniPlVisitor visitor) {
			value.visit(visitor);
			visitor.visit(this);
		}
	}
	
	public record BuiltinRead(
			@TokenValue("variable") String variable
	) implements Statement {
		
		public static final Input PATTERN = Input.allOf(
				Input.token(MiniPlTokenType.BUILTIN_READ),
				Input.token("variable", MiniPlTokenType.IDENTIFIER),
				STATEMENT_END
				);
		
		@Override
		public void visit(MiniPlVisitor visitor) {
			visitor.visit(this);
		}
	}
	
	public record BuiltinPrint(
			@TokenValue("expr") Expression expr
	) implements Statement {
		
		public static final Input PATTERN = Input.allOf(
				Input.token(MiniPlTokenType.BUILTIN_PRINT),
				Input.virtualNode("expr", EXPRESSIONS),
				STATEMENT_END
				);
		
		@Override
		public void visit(MiniPlVisitor visitor) {
			expr.visit(visitor);
			visitor.visit(this);
		}
	}
	
	public record IfBlock(
			@ChildNode("condition") Expression condition,
			@ChildNode("body") Block body,
			@ChildNode("fallback") Block fallback
	) implements Statement {
		
		public static final Input PATTERN = Input.allOf(
				Input.token(MiniPlTokenType.IF),
				Input.virtualNode("condition", EXPRESSIONS),
				Input.token(MiniPlTokenType.BLOCK_DO),
				Input.childNode("body", Block.class),
				Input.optional(Input.allOf(
						Input.token(MiniPlTokenType.IF_ELSE),
						Input.childNode("fallback", Block.class)
						)),
				Input.token(MiniPlTokenType.BLOCK_END),
				Input.token(MiniPlTokenType.IF),
				STATEMENT_END
				);
		
		@Override
		public void visit(MiniPlVisitor visitor) {
			condition.visit(visitor);
			body.visit(visitor);
			if (fallback != null) {				
				fallback.visit(visitor);
			}
			visitor.visit(this);
		}
	}
	
	public record ForBlock(
			@TokenValue("counter") String counter,
			@ChildNode("start") Expression start,
			@ChildNode("end") Expression end,
			@ChildNode("body") Block body
	) implements Statement {
		
		public static final Input PATTERN = Input.allOf(
				Input.token(MiniPlTokenType.FOR),
				Input.token("counter", MiniPlTokenType.IDENTIFIER),
				Input.token(MiniPlTokenType.FOR_IN),
				Input.virtualNode("start", EXPRESSIONS),
				Input.token(MiniPlTokenType.FOR_DIVIDER),
				Input.virtualNode("end", EXPRESSIONS),
				Input.token(MiniPlTokenType.BLOCK_DO),
				Input.childNode("body", Block.class),
				Input.token(MiniPlTokenType.BLOCK_END),
				Input.token(MiniPlTokenType.FOR),
				STATEMENT_END
				);
		
		@Override
		public void visit(MiniPlVisitor visitor) {
			start.visit(visitor);
			end.visit(visitor);
			body.visit(visitor);
			visitor.visit(this);
		}
	}
	
	// Expressions
	public interface Expression extends Node {}
	
	public static final VirtualNode EXPRESSIONS = VirtualNode.parseOrError(MiniPlError.MISSING_EXPRESSION,
			Group.class, LogicalNotExpr.class, LogicalAndExpr.class, EqualsExpr.class, LessThanExpr.class,
			AddExpr.class, SubtractExpr.class, MultiplyExpr.class, DivideExpr.class,
			Literal.class);

	public record Literal(
			@ChildNode("value") Value value
	) implements Expression {
		
		public static final Input PATTERN = Input.childNode("value", VALUES);
		
		@Override
		public void visit(MiniPlVisitor visitor) {
			value.visit(visitor);
			visitor.visit(this);
		}
	}
	
	public record Group(
			@ChildNode("expr") Expression expr
	) implements Expression {
		
		public static final Input PATTERN = Input.allOf(
				Input.token(MiniPlTokenType.GROUP_BEGIN),
				Input.virtualNode("expr", EXPRESSIONS),
				Input.token(MiniPlTokenType.GROUP_END)
				);
		
		@Override
		public void visit(MiniPlVisitor visitor) {
			expr.visit(visitor);
			visitor.visit(this);
		}
	}
	
	public record LogicalNotExpr(
			@ChildNode("expr") Expression expr
	) implements Expression {
		
		public static final Input PATTERN = Input.allOf(
				Input.token(MiniPlTokenType.LOGICAL_NOT),
				Input.virtualNode("expr", EXPRESSIONS)
				);
		
		@Override
		public void visit(MiniPlVisitor visitor) {
			expr.visit(visitor);
			visitor.visit(this);
		}
	}
	
	public record LogicalAndExpr(
			@ChildNode("lhs") Expression lhs,
			@ChildNode("rhs") Expression rhs
	) implements Expression {
		
		public static final Input PATTERN = Input.allOf(
				Input.virtualNode("lhs", EXPRESSIONS),
				Input.token(MiniPlTokenType.LOGICAL_AND),
				Input.virtualNode("rhs", EXPRESSIONS)
				);
		
		@Override
		public void visit(MiniPlVisitor visitor) {
			lhs.visit(visitor);
			rhs.visit(visitor);
			visitor.visit(this);
		}
	}
	
	public record EqualsExpr(
			@ChildNode("lhs") Expression lhs,
			@ChildNode("rhs") Expression rhs
	) implements Expression {
		
		public static final Input PATTERN = Input.allOf(
				Input.virtualNode("lhs", EXPRESSIONS),
				Input.token(MiniPlTokenType.EQUALS),
				Input.virtualNode("rhs", EXPRESSIONS)
				);
		
		@Override
		public void visit(MiniPlVisitor visitor) {
			lhs.visit(visitor);
			rhs.visit(visitor);
			visitor.visit(this);
		}
	}
	
	public record LessThanExpr(
			@ChildNode("lhs") Expression lhs,
			@ChildNode("rhs") Expression rhs
	) implements Expression {
		
		public static final Input PATTERN = Input.allOf(
				Input.virtualNode("lhs", EXPRESSIONS),
				Input.token(MiniPlTokenType.LESS_THAN),
				Input.virtualNode("rhs", EXPRESSIONS)
				);
		
		@Override
		public void visit(MiniPlVisitor visitor) {
			lhs.visit(visitor);
			rhs.visit(visitor);
			visitor.visit(this);
		}
	}
	
	public record AddExpr(
			@ChildNode("lhs") Expression lhs,
			@ChildNode("rhs") Expression rhs
	) implements Expression {
		
		public static final Input PATTERN = Input.allOf(
				Input.virtualNode("lhs", EXPRESSIONS),
				Input.token(MiniPlTokenType.ADD),
				Input.virtualNode("rhs", EXPRESSIONS)
				);
		
		@Override
		public void visit(MiniPlVisitor visitor) {
			lhs.visit(visitor);
			rhs.visit(visitor);
			visitor.visit(this);
		}
	}
	
	public record SubtractExpr(
			@ChildNode("lhs") Expression lhs,
			@ChildNode("rhs") Expression rhs
	) implements Expression {
		
		public static final Input PATTERN = Input.allOf(
				Input.virtualNode("lhs", EXPRESSIONS),
				Input.token(MiniPlTokenType.SUBTRACT),
				Input.virtualNode("rhs", EXPRESSIONS)
				);
		
		@Override
		public void visit(MiniPlVisitor visitor) {
			lhs.visit(visitor);
			rhs.visit(visitor);
			visitor.visit(this);
		}
	}
	
	public record MultiplyExpr(
			@ChildNode("lhs") Expression lhs,
			@ChildNode("rhs") Expression rhs
	) implements Expression {
		
		public static final Input PATTERN = Input.allOf(
				Input.virtualNode("lhs", EXPRESSIONS),
				Input.token(MiniPlTokenType.MULTIPLY),
				Input.virtualNode("rhs", EXPRESSIONS)
				);
		
		@Override
		public void visit(MiniPlVisitor visitor) {
			lhs.visit(visitor);
			rhs.visit(visitor);
			visitor.visit(this);
		}
	}
	
	public record DivideExpr(
			@ChildNode("lhs") Expression lhs,
			@ChildNode("rhs") Expression rhs
	) implements Expression {
		
		public static final Input PATTERN = Input.allOf(
				Input.virtualNode("lhs", EXPRESSIONS),
				Input.token(MiniPlTokenType.DIVIDE),
				Input.virtualNode("rhs", EXPRESSIONS)
				);
		
		@Override
		public void visit(MiniPlVisitor visitor) {
			lhs.visit(visitor);
			rhs.visit(visitor);
			visitor.visit(this);
		}
	}
			
	// Values
	public interface Value extends Node {}
	
	public static final List<Class<? extends AstNode>> VALUES = List.of(Constant.class, VarReference.class);
	
	public record Constant(
			@TokenValue("value") Object value
	) implements Value {
		
		public static final Input PATTERN = Input.oneOf(
				Input.token("value", MiniPlTokenType.INT_LITERAL),
				Input.token("value", MiniPlTokenType.STRING_LITERAL)
				);
		
		@Override
		public void visit(MiniPlVisitor visitor) {
			visitor.visit(this);
		}
	}
	
	public record VarReference(
			@TokenValue("variable") String variable
	) implements Value {
		
		public static final Input PATTERN = Input.token("variable", MiniPlTokenType.IDENTIFIER);
		
		@Override
		public void visit(MiniPlVisitor visitor) {
			visitor.visit(this);
		}
	}
	
	static {
		REGISTRY.register(Program.class, Block.class);
		REGISTRY.register(STATEMENTS);
		REGISTRY.register(EXPRESSIONS.astNodeTypes());
		REGISTRY.register(VALUES);
	}
}
