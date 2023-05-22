package fi.benjami.code4jvm.lua.parser;

import java.util.ArrayList;
import java.util.List;

import fi.benjami.code4jvm.lua.ir.IrNode;
import fi.benjami.code4jvm.lua.ir.LuaType;
import fi.benjami.code4jvm.lua.ir.expr.FunctionCallExpr;
import fi.benjami.code4jvm.lua.ir.expr.LuaConstant;
import fi.benjami.code4jvm.lua.ir.expr.TableInitExpr;
import fi.benjami.code4jvm.lua.ir.expr.VariableExpr;
import fi.benjami.code4jvm.lua.semantic.LuaScope;
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
			BinaryExpr.Equal.class, BinaryExpr.NotEqual.class,
			BinaryExpr.LessThan.class, BinaryExpr.LessOrEqual.class, // Precedence 9
			BinaryExpr.MoreThan.class, BinaryExpr.MoreOrEqual.class,
			// TODO bitwise operations, not implemented and disabled to save stack
//			BinaryExpr.BitwiseOr.class, // Precedence 8
//			BinaryExpr.BitwiseXor.class, // Precedence 7
//			BinaryExpr.BitwiseAnd.class, // Precedence 6
//			BinaryExpr.BitShiftLeft.class, BinaryExpr.BitShiftRight.class, // Precedence 5
			BinaryExpr.StringConcat.class, // Precedence 4
			BinaryExpr.Add.class, BinaryExpr.Subtract.class, // Precedence 3
			BinaryExpr.Multiply.class, BinaryExpr.Divide.class, BinaryExpr.FloorDivide.class, BinaryExpr.Modulo.class, // Precedence 2
			// Unary expressions have higher precedence than binary expressions (1)
			UnaryExpr.LogicalNot.class, UnaryExpr.ArrayLength.class,
//			UnaryExpr.Negate.class, UnaryExpr.BitwiseNot.class,			
			BinaryExpr.Power.class, // Exponentiation (precedence 0)
			
			FunctionCall.class, // FIXME IMPORTANT: due to caching wonkiness, this MUST be after binary expressions
			SimpleConstant.class, StringConstant.class,
			VarArgs.class, FunctionDefinition.class, // Generic expressions
			VarReference.class, Group.class, // Prefix expressions
			TableConstructor.class // Generic expressions, continued
			);
	
	public static VirtualNode PREFIX_EXPRS = VirtualNode.of(
			VarReference.class, FunctionCall.class, Group.class
			);

	public interface Constant extends Expression {
		
		Object value();
	}
	
	public record StringConstant(
			@TokenValue("value") String value
	) implements Constant {
		
		public static final Input PATTERN = Input.token("value", LuaToken.STRING_LITERAL);
		
		@Override
		public IrNode toIr(LuaScope scope) {
			return new LuaConstant(value, LuaType.STRING);
		}
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
		
		@Override
		public IrNode toIr(LuaScope scope) {
			return new LuaConstant(value, LuaType.of(value));
		}
	}
	
	public record VarArgs() implements Expression {
		
		public static final Input PATTERN = Input.token(LuaToken.VARARGS);
		
		@Override
		public IrNode toIr(LuaScope scope) {
			throw new UnsupportedOperationException();
		}
	}
	
	public record FunctionDefinition(
			@ChildNode("body") SpecialNodes.FunctionBody body
	) implements Expression {
		
		public static final Input PATTERN = Input.allOf(
				Input.token(LuaToken.FUNCTION),
				Input.childNode("body", SpecialNodes.FunctionBody.class)
				);
		
		@Override
		public IrNode toIr(LuaScope scope) {
			return body.toIr(scope);
		}
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
		
		@Override
		public VariableExpr toIr(LuaScope scope) {
			// First part may refer to a local variable, a
			var node = new VariableExpr(scope.resolve(parts.get(0)));
			// Table lookup separated by dots, a.b.c
			for (var i = 1; i < parts.size(); i++) {
				node = new VariableExpr(new fi.benjami.code4jvm.lua.ir.TableField(node, new LuaConstant(parts.get(i))));
			}
			// Dynamic table access, a.b.c["foo"]
			if (tableIndex != null) {
				node = new VariableExpr(new fi.benjami.code4jvm.lua.ir.TableField(node, tableIndex.toIr(scope)));
			}
			return node;
		}
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
		
		@Override
		public IrNode toIr(LuaScope scope) {
			var function = path.toIr(scope);
			var irArgs = new ArrayList<IrNode>();
			if (oopCallName != null) {
				// Syntactic sugar for "OOP"; table:func(...) -> table.func(table, ...)
				irArgs.add(function); // i.e. the table is first argument
				function = new VariableExpr(new fi.benjami.code4jvm.lua.ir.TableField(function, new LuaConstant(oopCallName)));
			}
			irArgs.addAll(args.stream().map(arg -> arg.toIr(scope)).toList()); // Rest of arguments
			return new FunctionCallExpr(function, irArgs);
		}
	}
	
	public record Group(
			@ChildNode("expr") Expression expr
	) implements PrefixExpr {
		
		public static final Input PATTERN = Input.allOf(
				Input.token(LuaToken.GROUP_BEGIN),
				Input.virtualNode("expr", EXPRESSIONS),
				Input.token(LuaToken.GROUP_END)
				);
		
		@Override
		public IrNode toIr(LuaScope scope) {
			return expr.toIr(scope);
		}
	}
	
	public record TableConstructor(
			@ChildNode("fields") List<TableField> fields
	) implements Expression {
		
		public static final Input PATTERN = Input.allOf(
				Input.token(LuaToken.TABLE_INIT_START),
				Input.list(Input.childNode("fields", TableField.class), 0, Input.token(LuaToken.LIST_SEPARATOR)),
				Input.optional(Input.token(LuaToken.LIST_SEPARATOR)), // One extra comma allowed
				Input.token(LuaToken.TABLE_INIT_END)
				);
		
		@Override
		public IrNode toIr(LuaScope scope) {
			var entries = new ArrayList<TableInitExpr.Entry>(fields.size());
			var arrayIndex = 1d;
			for (var field : fields) {
				IrNode key;
				if (field.name == null) {
					if (field.nameExpr == null) {
						key = new LuaConstant(arrayIndex++);
					} else {
						key = field.nameExpr.toIr(scope);
					}
				} else {
					key = new LuaConstant(field.name);
				}
				entries.add(new TableInitExpr.Entry(key, field.value.toIr(scope)));
			}
			return new TableInitExpr(entries);
		}
	}
	
	public record TableField(
			@TokenValue("name") String name,
			@ChildNode("nameExpr") Expression nameExpr,
			@ChildNode("value") Expression value
	) implements LuaNode {
		
		public static final Input PATTERN = Input.oneOf(
				Input.allOf(
						Input.token(LuaToken.TABLE_INDEX_BEGIN),
						Input.virtualNode("nameExpr", EXPRESSIONS),
						Input.token(LuaToken.TABLE_INDEX_END),
						Input.token(LuaToken.ASSIGNMENT),
						Input.virtualNode("value", EXPRESSIONS)
						),
				Input.allOf(
						Input.token("name", LuaToken.NAME),
						Input.token(LuaToken.ASSIGNMENT),
						Input.virtualNode("value", EXPRESSIONS)
						),
				Input.virtualNode("value", EXPRESSIONS)
				);
		
		@Override
		public IrNode toIr(LuaScope scope) {
			throw new AssertionError(); // TableConstructor#toIr handles this
		}
	}
}
