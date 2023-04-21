package fi.benjami.code4jvm.lua.parser;

import java.util.List;

import fi.benjami.parserkit.parser.Input;
import fi.benjami.parserkit.parser.VirtualNode;
import fi.benjami.parserkit.parser.ast.ChildNode;
import fi.benjami.parserkit.parser.ast.TokenValue;

public interface Expression extends LuaNode {
	
	// Nested expression parsing is allowed to fail!
	public static VirtualNode EXPRESSIONS = VirtualNode.of(
			// Expressions are in priority order (highest to lowest)
			// To implement maximal munch rule, the order is generally
			// 1. Binary expressions
			// 2. Unary expressions
			// 3. Other expressions
			// This ensures that e.g. a + b is parsed as Add(Constant(a), Constant(b)) instead of
			// just Constant(a) and a parse error
			
			// Binary and unary expressions can be nested
			// Their predecence is set as described in Lua spec
			// Lowest precedence first
			BinaryExpr.LogicalOr.class, // Precedence 11
			BinaryExpr.LogicalAnd.class, // Precedence 10
			BinaryExpr.LessThan.class, BinaryExpr.LessOrEqual.class, // Precedence 9
			BinaryExpr.MoreThan.class, BinaryExpr.MoreOrEqual.class,
			BinaryExpr.Equal.class, BinaryExpr.NotEqual.class,
			BinaryExpr.BitwiseOr.class, // Precedence 8
			BinaryExpr.BitwiseXor.class, // Precedence 7
			BinaryExpr.BitwiseAnd.class, // Precedence 6
			BinaryExpr.BitShiftLeft.class, BinaryExpr.BitShiftRight.class, // Precedence 5
			BinaryExpr.StringConcat.class, // Precedence 4
			BinaryExpr.Add.class, BinaryExpr.Subtract.class, // Precedence 3
			BinaryExpr.Multiply.class, BinaryExpr.Divide.class, BinaryExpr.FloorDivide.class, BinaryExpr.Modulo.class, // Precedence 2
			// Unary expressions have higher precedence than binary expressions (1)
			UnaryExpr.LogicalNot.class, UnaryExpr.ArrayLength.class,
			UnaryExpr.Negate.class, UnaryExpr.BitwiseNot.class,			
			BinaryExpr.Power.class, // Exponentiation (precedence 0)
			
			SimpleConstant.class, StringConstant.class,
			VarArgs.class, FunctionDefinition.class, // Generic expressions
			VarReference.class,  Group.class // Prefix expressions
//			TableConstructor.class, // Generic expressions, continued
			);
	
	public static VirtualNode PREFIX_EXPRS = VirtualNode.parseOrError(0,
			VarReference.class, FunctionCall.class, Group.class
			);

	public interface Constant extends Expression {
		
		Object value();
	}
	
	public record StringConstant(
			@TokenValue("value") String value
	) implements Constant {
		
		public static final Input PATTERN = Input.token("value", LuaToken.STRING_LITERAL);
	}
	
	public record SimpleConstant(
			@TokenValue("value") Object value
	) implements Constant {
		
		public static final Input PATTERN = Input.oneOf(
				Input.token("value", LuaToken.LITERAL_NUMBER),
				Input.token("value", LuaToken.LITERAL_NIL),
				Input.token("value", LuaToken.LITERAL_FALSE),
				Input.token("value", LuaToken.LITERAL_TRUE)
				); 
	}
	
	public record VarArgs() implements Expression {
		
		public static final Input PATTERN = Input.token(LuaToken.VARARGS);
	}
	
	public record FunctionDefinition(
			@ChildNode("body") SpecialNodes.FunctionBody body
	) implements Expression {
		
		public static final Input PATTERN = Input.allOf(
				Input.token(LuaToken.FUNCTION),
				Input.childNode("body", SpecialNodes.FunctionBody.class)
				);
	}
	
	public interface PrefixExpr extends Expression {
		
	}
	
	public record VarReference(
			@ChildNode("parts") List<String> parts,
			@ChildNode("tableIndex") Expression tableIndex
	) implements PrefixExpr {
		
		public static final Input PATTERN = Input.allOf(
				Input.list(
						Input.token("parts", LuaToken.NAME),
						1,
						Input.token(LuaToken.NAME_SEPARATOR)
						),
				Input.optional(Input.allOf(
						Input.token(LuaToken.TABLE_INDEX_BEGIN),
						Input.virtualNode("tableIndex", EXPRESSIONS),
						Input.token(LuaToken.TABLE_INDEX_END)
						))
				);
	}
	
	public record FunctionCall(
			@ChildNode("path") PrefixExpr path,
			@TokenValue("name") String oopCallName,
			@ChildNode("args") List<Expression> args
	) implements PrefixExpr, Statement {
		
		public static final Input PATTERN = Input.allOf(
				Input.virtualNode("path", PREFIX_EXPRS),
				Input.optional(Input.allOf(
						Input.token(LuaToken.OOP_FUNC_SEPARATOR),
						Input.token("name", LuaToken.NAME)
						)),
				Input.oneOf(
						Input.allOf(
								Input.token(LuaToken.GROUP_BEGIN),
								Input.list(
										Input.virtualNode("args", EXPRESSIONS),
										0,
										Input.token(LuaToken.LIST_SEPARATOR)
										),
								Input.token(LuaToken.GROUP_END)
								), // TODO table constructor
						Input.childNode("args", StringConstant.class)
						)
				);
	}
	
	public record Group(
			@ChildNode("expr") Expression expr
	) implements PrefixExpr {
		
		public static final Input PATTERN = Input.allOf(
				Input.token(LuaToken.GROUP_BEGIN),
				Input.virtualNode("expr", EXPRESSIONS),
				Input.token(LuaToken.GROUP_END)
				);
	}
	
	public record TableConstructor(
			@ChildNode("fields") List<TableField> fields
	) implements Expression {
		
	}
	
	public record TableField(
			@TokenValue("name") String name,
			@ChildNode("nameExpr") Expression nameExpr,
			@ChildNode("value") Expression value
	) implements LuaNode {
		
	}
}
