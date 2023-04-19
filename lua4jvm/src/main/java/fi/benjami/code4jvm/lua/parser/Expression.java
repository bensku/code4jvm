package fi.benjami.code4jvm.lua.parser;

import java.util.List;

import fi.benjami.parserkit.parser.Input;
import fi.benjami.parserkit.parser.VirtualNode;
import fi.benjami.parserkit.parser.ast.ChildNode;
import fi.benjami.parserkit.parser.ast.TokenValue;

public interface Expression extends LuaNode {
	
	public static VirtualNode EXPRESSIONS = VirtualNode.parseOrError(0,
			SimpleConstant.class, StringConstant.class,
			VarArgs.class, FunctionDefinition.class, // Generic expressions
			VarReference.class, FunctionCall.class, Group.class, // Prefix expressions
//			TableConstructor.class, // Generic expressions, continued
			BinaryExpr.Power.class, // Exponentiation (priority 0)
			// Unary expressions have higher priority than binary expressions (priority 1)
			UnaryExpr.LogicalNot.class, UnaryExpr.ArrayLength.class,
			UnaryExpr.Negate.class, UnaryExpr.BitwiseNot.class,
			// Binary expressions have their priority set according to Lua specification
			BinaryExpr.Multiply.class, BinaryExpr.Divide.class, BinaryExpr.FloorDivide.class, BinaryExpr.Modulo.class, // Priority 2
			BinaryExpr.Add.class, BinaryExpr.Subtract.class, // Priority 3
			BinaryExpr.StringConcat.class, // Priority 4
			BinaryExpr.BitShiftLeft.class, BinaryExpr.BitShiftRight.class, // Priority 5
			BinaryExpr.BitwiseAnd.class, // Priority 6
			BinaryExpr.BitwiseXor.class, // Priority 7
			BinaryExpr.BitwiseOr.class, // Priority 8
			BinaryExpr.LessThan.class, BinaryExpr.LessOrEqual.class, // Priority 9
			BinaryExpr.MoreThan.class, BinaryExpr.MoreOrEqual.class,
			BinaryExpr.Equal.class, BinaryExpr.NotEqual.class,
			BinaryExpr.LogicalAnd.class, // Priority 10
			BinaryExpr.LogicalOr.class // Priority 11
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
		
		public static final Input PATTERN = Input.token("value", LuaToken.LITERAL_NUMBER); // FIXME nil, false, true
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
						Input.virtualNode("tableIndex", EXPRESSIONS)
						))
				);
	}
	
	public record FunctionCall(
			@ChildNode("path") PrefixExpr path,
			@TokenValue("name") String name,
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
