package fi.benjami.parserkit.minipl;

import java.util.List;

import fi.benjami.parserkit.parser.Input;
import fi.benjami.parserkit.parser.NodeRegistry;
import fi.benjami.parserkit.parser.ast.AstNode;
import fi.benjami.parserkit.parser.ast.ChildNode;
import fi.benjami.parserkit.parser.ast.TokenValue;

public class MiniPlNodes {
	
	public static final NodeRegistry REGISTRY = new NodeRegistry();
	
	public record Program(
			@ChildNode("block") Block block
	) implements AstNode {
		
		public static final Input PATTERN = Input.childNode("block", Block.class);
	}
	
	public record Block(
			@ChildNode("stmt") List<Object> statements
	) implements AstNode {
		
		public static final Input PATTERN = Input.repeating(Input.allOf(
				Input.childNode("stmt", STATEMENTS),
				Input.token(MiniPlTokenType.STATEMENT_END)
				));
	}

	// Statements
	public static final List<Class<? extends AstNode>> STATEMENTS = List.of(VarDeclaration.class,
			VarAssignment.class, BuiltinRead.class, BuiltinPrint.class, IfBlock.class, ForBlock.class);
	
	public record VarDeclaration(
			@TokenValue("name") String name,
			@TokenValue("type") String type,
			@ChildNode("expr") Object initialValue
	) implements AstNode {
		
		public static final Input PATTERN = Input.allOf(
				Input.token(MiniPlTokenType.DECLARE_VAR),
				Input.token("name", MiniPlTokenType.IDENTIFIER),
				Input.token(MiniPlTokenType.VAR_TYPE),
				Input.token("type", MiniPlTokenType.IDENTIFIER),
				Input.optional(Input.allOf(
						Input.token(MiniPlTokenType.ASSIGNMENT),
						Input.childNode("expr", EXPRESSIONS)
						))
				);
	}
	
	public record VarAssignment(
			@TokenValue("name") String name,
			@ChildNode("expr") Object value
	) implements AstNode {
		
		public static final Input PATTERN = Input.allOf(
				Input.token("name", MiniPlTokenType.IDENTIFIER),
				Input.token(MiniPlTokenType.ASSIGNMENT),
				Input.childNode("expr", EXPRESSIONS)
				);
	}
	
	public record BuiltinRead(
			@TokenValue("function") String function,
			@TokenValue("variable") String variable
	) implements AstNode {
		
		public static final Input PATTERN = Input.allOf(
				Input.token("function", MiniPlTokenType.BUILTIN_READ),
				Input.token("variable", MiniPlTokenType.IDENTIFIER)
				);
	}
	
	public record BuiltinPrint(
			@TokenValue("function") String function,
			@TokenValue("expr") Object expr
	) implements AstNode {
		
		public static final Input PATTERN = Input.allOf(
				Input.token("function", MiniPlTokenType.BUILTIN_PRINT),
				Input.childNode("expr", EXPRESSIONS)
				);
	}
	
	public record IfBlock(
			@ChildNode("condition") Object condition,
			@ChildNode("body") Block body,
			@ChildNode("fallback") Block fallback
	) implements AstNode {
		
		public static final Input PATTERN = Input.allOf(
				Input.token(MiniPlTokenType.IF),
				Input.childNode("condition", EXPRESSIONS),
				Input.token(MiniPlTokenType.BLOCK_DO),
				Input.childNode("body", Block.class),
				Input.optional(Input.allOf(
						Input.token(MiniPlTokenType.IF_ELSE),
						Input.childNode("fallback", Block.class)
						)),
				Input.token(MiniPlTokenType.BLOCK_END),
				Input.token(MiniPlTokenType.IF)
				);
	}
	
	public record ForBlock(
			@TokenValue("counter") String counter,
			@ChildNode("start") Object start,
			@ChildNode("end") Object end,
			@ChildNode("body") Block body
	) implements AstNode {
		
		public static final Input PATTERN = Input.allOf(
				Input.token(MiniPlTokenType.FOR),
				Input.token("counter", MiniPlTokenType.IDENTIFIER),
				Input.token(MiniPlTokenType.FOR_IN),
				Input.childNode("start", EXPRESSIONS),
				Input.token(MiniPlTokenType.FOR_DIVIDER),
				Input.childNode("end", EXPRESSIONS),
				Input.token(MiniPlTokenType.BLOCK_DO),
				Input.childNode("body", Block.class),
				Input.token(MiniPlTokenType.BLOCK_END),
				Input.token(MiniPlTokenType.FOR)
				);
	}
	
	// Expressions
	public static final List<Class<? extends AstNode>> EXPRESSIONS = List.of(Group.class,
			LogicalNotExpr.class, LogicalAndExpr.class, EqualsExpr.class, LessThanExpr.class,
			AddExpr.class, SubtractExpr.class, MultiplyExpr.class, DivideExpr.class,
			Literal.class);

	public record Literal(
			@ChildNode("value") Object value
	) implements AstNode {
		
		public static final Input PATTERN = Input.childNode("value", VALUES);
	}
	
	public record Group(
			@ChildNode("expr") Object expr
	) implements AstNode {
		
		public static final Input PATTERN = Input.allOf(
				Input.token(MiniPlTokenType.GROUP_BEGIN),
				Input.childNode("expr", EXPRESSIONS),
				Input.token(MiniPlTokenType.GROUP_END)
				);
	}
	
	public record LogicalNotExpr(
			@ChildNode("expr") Object expr
	) implements AstNode {
		
		public static final Input PATTERN = Input.allOf(
				Input.token(MiniPlTokenType.LOGICAL_NOT),
				Input.childNode("expr", EXPRESSIONS)
				);
	}
	
	public record LogicalAndExpr(
			@ChildNode("lhs") Object lhs,
			@ChildNode("rhs") Object rhs
	) implements AstNode {
		
		public static final Input PATTERN = Input.allOf(
				Input.childNode("lhs", EXPRESSIONS),
				Input.token(MiniPlTokenType.LOGICAL_AND),
				Input.childNode("rhs", EXPRESSIONS)
				);
	}
	
	public record EqualsExpr(
			@ChildNode("lhs") Object lhs,
			@ChildNode("rhs") Object rhs
	) implements AstNode {
		
		public static final Input PATTERN = Input.allOf(
				Input.childNode("lhs", EXPRESSIONS),
				Input.token(MiniPlTokenType.EQUALS),
				Input.childNode("rhs", EXPRESSIONS)
				);
	}
	
	public record LessThanExpr(
			@ChildNode("lhs") Object lhs,
			@ChildNode("rhs") Object rhs
	) implements AstNode {
		
		public static final Input PATTERN = Input.allOf(
				Input.childNode("lhs", EXPRESSIONS),
				Input.token(MiniPlTokenType.LESS_THAN),
				Input.childNode("rhs", EXPRESSIONS)
				);
	}
	
	public record AddExpr(
			@ChildNode("lhs") Object lhs,
			@ChildNode("rhs") Object rhs
	) implements AstNode {
		
		public static final Input PATTERN = Input.allOf(
				Input.childNode("lhs", EXPRESSIONS),
				Input.token(MiniPlTokenType.ADD),
				Input.childNode("rhs", EXPRESSIONS)
				);
	}
	
	public record SubtractExpr(
			@ChildNode("lhs") Object lhs,
			@ChildNode("rhs") Object rhs
	) implements AstNode {
		
		public static final Input PATTERN = Input.allOf(
				Input.childNode("lhs", EXPRESSIONS),
				Input.token(MiniPlTokenType.SUBTRACT),
				Input.childNode("rhs", EXPRESSIONS)
				);
	}
	
	public record MultiplyExpr(
			@ChildNode("lhs") Object lhs,
			@ChildNode("rhs") Object rhs
	) implements AstNode {
		
		public static final Input PATTERN = Input.allOf(
				Input.childNode("lhs", EXPRESSIONS),
				Input.token(MiniPlTokenType.MULTIPLY),
				Input.childNode("rhs", EXPRESSIONS)
				);
	}
	
	public record DivideExpr(
			@ChildNode("lhs") Object lhs,
			@ChildNode("rhs") Object rhs
	) implements AstNode {
		
		public static final Input PATTERN = Input.allOf(
				Input.childNode("lhs", EXPRESSIONS),
				Input.token(MiniPlTokenType.DIVIDE),
				Input.childNode("rhs", EXPRESSIONS)
				);
	}
			
	// Values
	public static final List<Class<? extends AstNode>> VALUES = List.of(Constant.class, VarReference.class);
	
	public record Constant(
			@TokenValue("value") Object value
	) implements AstNode {
		
		public static final Input PATTERN = Input.oneOf(
				Input.token("value", MiniPlTokenType.INT_LITERAL),
				Input.token("value", MiniPlTokenType.STRING_LITERAL)
				);
	}
	
	public record VarReference(
			@TokenValue("variable") String variable
	) implements AstNode {
		
		public static final Input PATTERN = Input.token(MiniPlTokenType.IDENTIFIER);
	}
	
	static {
		REGISTRY.register(Program.class, Block.class);
		REGISTRY.register(STATEMENTS);
		REGISTRY.register(EXPRESSIONS);
		REGISTRY.register(VALUES);
	}
}
