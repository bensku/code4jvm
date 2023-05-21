package fi.benjami.code4jvm.lua.parser;

import java.util.ArrayList;
import java.util.List;

import fi.benjami.code4jvm.Value;
import fi.benjami.code4jvm.block.Block;
import fi.benjami.code4jvm.lua.compiler.LuaContext;
import fi.benjami.code4jvm.lua.ir.IrNode;
import fi.benjami.code4jvm.lua.ir.LuaType;
import fi.benjami.code4jvm.lua.ir.LuaVariable;
import fi.benjami.code4jvm.lua.ir.expr.LuaConstant;
import fi.benjami.code4jvm.lua.ir.expr.VariableExpr;
import fi.benjami.code4jvm.lua.ir.stmt.ReturnStmt;
import fi.benjami.code4jvm.lua.ir.stmt.SetVariablesStmt;
import fi.benjami.code4jvm.lua.semantic.LuaScope;
import fi.benjami.parserkit.parser.Input;
import fi.benjami.parserkit.parser.VirtualNode;
import fi.benjami.parserkit.parser.ast.ChildNode;
import fi.benjami.parserkit.parser.ast.TokenValue;

public interface Statement extends LuaNode {

	// FIXME statement level error recovery has unusably bad performance
	public static final VirtualNode STATEMENTS = VirtualNode.of(
			Empty.class, VarAssignment.class, Expression.FunctionCall.class,
			Label.class, LoopBreak.class, Goto.class,
			DoEndBlock.class, WhileLoop.class, RepeatLoop.class, IfBlock.class,
//			CountingForLoop.class, IteratorForLoop.class,
			FunctionDeclaration.class,
			LocalVarDeclaration.class, LocalFunctionDeclaration.class,
			Return.class
			);
	
	public record Empty() implements Statement {
		
		public static final Input PATTERN = Input.token(LuaToken.STATEMENT_END);
		
		@Override
		public IrNode toIr(LuaScope scope) {
			return new IrNode() {
				
				@Override
				public LuaType outputType(LuaContext ctx) {
					return LuaType.NIL;
				}
				
				@Override
				public Value emit(LuaContext ctx, Block block) {
					return null;
				}
			};
		}
	}
	
	public record VarAssignment(
			@ChildNode("variables") List<Expression.VarReference> variables,
			@ChildNode("values") List<Expression> values
	) implements Statement {
		
		public static final Input PATTERN = Input.allOf(
				Input.list(
						Input.childNode("variables", Expression.VarReference.class),
						1,
						Input.token(LuaToken.LIST_SEPARATOR)
						),
				Input.token(LuaToken.ASSIGNMENT),
				Input.list(
						Input.virtualNode("values", Expression.EXPRESSIONS),
						1,
						Input.token(LuaToken.LIST_SEPARATOR)
						)
				);
		
		@Override
		public IrNode toIr(LuaScope scope) {
			var spread = values.size() == 1 && values.get(0) instanceof Expression.FunctionCall;
			var targets = variables.stream()
					.map(expr -> expr.toIr(scope))
					.map(VariableExpr::source)
					.toList();
			var sources = values.stream()
					.map(expr -> expr.toIr(scope))
					.toList();
			return new SetVariablesStmt(targets, sources, spread);
		}
	}
	
	public record LocalVarDeclaration(
			@TokenValue("varNames") List<String> varNames,
			@ChildNode("values") List<Expression> values
	) implements Statement {
		
		public static final Input PATTERN = Input.allOf(
				Input.token(LuaToken.LOCAL),
				// TODO Lua 5.4 local variable attributes
				Input.list(
						Input.token("varNames", LuaToken.NAME),
						1,
						Input.token(LuaToken.LIST_SEPARATOR)
						),
				Input.optional(Input.allOf(
						Input.token(LuaToken.ASSIGNMENT),
						Input.list(
								Input.virtualNode("values", Expression.EXPRESSIONS),
								1,
								Input.token(LuaToken.LIST_SEPARATOR)
								)
						))
				);
		
		@Override
		public IrNode toIr(LuaScope scope) {
			var spread = values.size() == 1 && values.get(0) instanceof Expression.FunctionCall;
			var targets = varNames.stream()
					// need to cast, List<LuaLocalVar> != List<LuaVariable> for javac
					.map(name -> (LuaVariable) scope.declare(name))
					.toList();
			var sources = values.stream()
					.map(expr -> expr.toIr(scope))
					.toList();
			return new SetVariablesStmt(targets, sources, spread);
		}
	}
	
	public record Label(
			@TokenValue("name") String name
	) implements Statement {
		
		public static final Input PATTERN = Input.allOf(
				Input.token(LuaToken.GOTO_LABEL),
				Input.token("name", LuaToken.NAME),
				Input.token(LuaToken.GOTO_LABEL)
				);
		
		@Override
		public IrNode toIr(LuaScope scope) {
			throw new UnsupportedOperationException();
		}
	}
	
	public record LoopBreak() implements Statement {
		
		public static final Input PATTERN = Input.token(LuaToken.LOOP_BREAK);
		
		@Override
		public IrNode toIr(LuaScope scope) {
			throw new UnsupportedOperationException();
		}
	}
	
	public record Goto(
			@TokenValue("label") String label
	) implements Statement {
		
		public static final Input PATTERN = Input.allOf(
				Input.token(LuaToken.GOTO_JUMP),
				Input.token("label", LuaToken.NAME)
				);
		
		@Override
		public IrNode toIr(LuaScope scope) {
			throw new UnsupportedOperationException();
		}
	}
	
	public record DoEndBlock(
			@ChildNode("block") SpecialNodes.Block block
	) implements Statement {
		
		public static final Input PATTERN = Input.allOf(
				Input.token(LuaToken.DO),
				Input.childNode("block", SpecialNodes.Block.class),
				Input.token(LuaToken.END)
				);
		
		@Override
		public IrNode toIr(LuaScope scope) {
			return block.toIr(new LuaScope(scope, false));
		}
	}
	
	public record WhileLoop(
			@ChildNode("condition") Expression condition,
			@ChildNode("body") SpecialNodes.Block body
	) implements Statement {
		
		public static final Input PATTERN = Input.allOf(
				Input.token(LuaToken.WHILE_LOOP),
				Input.virtualNode("condition", Expression.EXPRESSIONS),
				Input.token(LuaToken.DO),
				Input.childNode("body", SpecialNodes.Block.class),
				Input.token(LuaToken.END)
				);
		
		@Override
		public IrNode toIr(LuaScope scope) {
			throw new UnsupportedOperationException();
		}
	}
	
	public record RepeatLoop(
			@ChildNode("body") SpecialNodes.Block body,
			@ChildNode("condition") Expression condition
	) implements Statement {
		
		public static final Input PATTERN = Input.allOf(
				Input.token(LuaToken.REPEAT_LOOP),
				Input.childNode("body", SpecialNodes.Block.class),
				Input.token(LuaToken.REPEAT_UNTIL),
				Input.virtualNode("condition", Expression.EXPRESSIONS)
				);
		
		@Override
		public IrNode toIr(LuaScope scope) {
			throw new UnsupportedOperationException();
		}
	}
	
	public record IfBlock(
			@ChildNode("condition") Expression condition,
			@ChildNode("block") SpecialNodes.Block block,
			@ChildNode("elseIfs") List<ElseIfClause> elseIfs,
			@ChildNode("fallback") SpecialNodes.Block fallback
	) implements Statement {
		
		public static final Input PATTERN = Input.allOf(
				Input.token(LuaToken.IF_BLOCK),
				Input.virtualNode("condition", Expression.EXPRESSIONS),
				Input.token(LuaToken.IF_THEN),
				Input.childNode("block", SpecialNodes.Block.class),
				Input.repeating(Input.childNode("elseIfs", ElseIfClause.class)),
				Input.optional(Input.allOf(
						Input.token(LuaToken.IF_ELSE),
						Input.childNode("fallback", SpecialNodes.Block.class)
						)),
				Input.token(LuaToken.END)
				);
		
		@Override
		public IrNode toIr(LuaScope scope) {
			throw new UnsupportedOperationException();
		}
	}
	
	public record ElseIfClause(
			@ChildNode("condition") Expression condition,
			@ChildNode("block") SpecialNodes.Block block
	) implements LuaNode {
		
		public static final Input PATTERN = Input.allOf(
				Input.token(LuaToken.IF_ELSE_IF),
				Input.virtualNode("condition", Expression.EXPRESSIONS),
				Input.token(LuaToken.IF_THEN),
				Input.childNode("block", SpecialNodes.Block.class)
				);
		
		@Override
		public IrNode toIr(LuaScope scope) {
			throw new UnsupportedOperationException();
		}
	}
	
//	public record CountingForLoop(
//			
//	) implements Statement {
//		
//	}
	
//	public record IteratorForLoop(
//			
//	) implements Statement {
//		
//	}
	
	public record FunctionDeclaration(
			@ChildNode("name") FunctionName name,
			@ChildNode("body") SpecialNodes.FunctionBody function 
	) implements Statement {
		
		public static final Input PATTERN = Input.allOf(
				Input.token(LuaToken.FUNCTION),
				Input.childNode("name", FunctionName.class),
				Input.childNode("body", SpecialNodes.FunctionBody.class)
				);
		
		@Override
		public IrNode toIr(LuaScope scope) {
			// Convert function name
			var target = new VariableExpr(scope.resolve(name.parts.get(0)));
			for (var i = 1; i < name.parts.size(); i++) {
				target = new VariableExpr(new fi.benjami.code4jvm.lua.ir.TableField(target, new LuaConstant(name.parts.get(i))));
			}
			var func = function;
			if (name.oopPart != null) {
				target = new VariableExpr(new fi.benjami.code4jvm.lua.ir.TableField(target, new LuaConstant(name.oopPart)));
				// Add implicit self argument
				var args = new ArrayList<String>(func.paramNames().size() + 1);
				args.add("self");
				args.addAll(func.paramNames());
				func = new SpecialNodes.FunctionBody(args, func.block());
			}
			
			return new SetVariablesStmt(List.of(target.source()), List.of(function.toIr(scope)), false);
		}
	}
	
	public record FunctionName(
			@TokenValue("parts") List<String> parts,
			@TokenValue("oopPart") String oopPart
	) implements Statement {
		
		public static final Input PATTERN = Input.allOf(
				Input.list(
						Input.token("parts", LuaToken.NAME),
						0,
						Input.token(LuaToken.NAME_SEPARATOR)
						),
				Input.optional(Input.allOf(
						Input.token(LuaToken.OOP_FUNC_SEPARATOR),
						Input.token("oopPart", LuaToken.NAME)
						))
				);
		
		@Override
		public IrNode toIr(LuaScope scope) {
			throw new AssertionError();
		}
	}
	
	public record LocalFunctionDeclaration(
			@TokenValue("name") String name,
			@ChildNode("body") SpecialNodes.FunctionBody function
	) implements Statement {
		
		public static final Input PATTERN = Input.allOf(
				Input.token(LuaToken.LOCAL),
				Input.token(LuaToken.FUNCTION),
				Input.token("name", LuaToken.NAME),
				Input.childNode("body", SpecialNodes.FunctionBody.class)
				);
		
		@Override
		public IrNode toIr(LuaScope scope) {
			var target = scope.declare(name);
			return new SetVariablesStmt(List.of(target), List.of(function.toIr(scope)), false);
		}
	}
	
	// Official grammar states that return is not a statement, but this
	// is probably best detected during semantic analysis
	public record Return(
			@ChildNode("values") List<Expression> values
	) implements Statement {
		
		public static final Input PATTERN = Input.allOf(
				Input.token(LuaToken.RETURN),
				Input.list(
						Input.virtualNode("values", Expression.EXPRESSIONS),
						0,
						Input.token(LuaToken.LIST_SEPARATOR)
						)
				);
		
		@Override
		public IrNode toIr(LuaScope scope) {
			return new ReturnStmt(values.stream()
					.map(expr -> expr.toIr(scope))
					.toList());
		}
	}
}
