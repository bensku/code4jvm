package fi.benjami.code4jvm.lua.parser;

import java.util.List;

import fi.benjami.code4jvm.lua.parser.SpecialNodes.FunctionBody;
import fi.benjami.code4jvm.lua.parser.SpecialNodes.TableField;
import fi.benjami.parserkit.parser.VirtualNode;
import fi.benjami.parserkit.parser.ast.ChildNode;
import fi.benjami.parserkit.parser.ast.TokenValue;

public interface Expression extends LuaNode {
	
	public static VirtualNode EXPRESSIONS = VirtualNode.parseOrError(0,
			Constant.class, VarArgs.class, FunctionDefinition.class, // Generic expressions
			VarReference.class, FunctionCall.class, Group.class, // Prefix expressions
			TableConstructor.class, // Generic expressions, continued
			BinaryExpr.Power.class, // Exponentiation (priority 0)
			// Unary expressions have higher priority than binary expressions (priority 1)
			UnaryExpr.Not.class, UnaryExpr.ArrayLength.class,
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

	public record Constant(
			@TokenValue("value") Object value
	) implements Expression {
		
	}
	
	public record VarArgs() implements Expression {
		
	}
	
	public record FunctionDefinition(
			@ChildNode("body") FunctionBody body
	) implements Expression {
		
	}
	
	public interface PrefixExpr extends Expression {
		
	}
	
	public record VarReference(
			@ChildNode("path") PrefixExpr path,
			@TokenValue("name") String name,
			@ChildNode("tableIndex") Expression tableIndex
	) implements PrefixExpr {
		
	}
	
	public record FunctionCall(
			@ChildNode("path") PrefixExpr path,
			@TokenValue("name") String name,
			@ChildNode("args") List<Expression> args
	) implements PrefixExpr, Statement {
		
	}
	
	public record Group(
			@ChildNode("expr") Expression expr
	) implements PrefixExpr {
		
	}
	
	public record TableConstructor(
			@ChildNode("fields") List<TableField> fields
	) implements Expression {
		
	}
}
