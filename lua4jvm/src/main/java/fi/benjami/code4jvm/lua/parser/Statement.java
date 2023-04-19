package fi.benjami.code4jvm.lua.parser;

import java.util.List;

import fi.benjami.parserkit.parser.Input;
import fi.benjami.parserkit.parser.VirtualNode;
import fi.benjami.parserkit.parser.ast.ChildNode;
import fi.benjami.parserkit.parser.ast.TokenValue;

public interface Statement extends LuaNode {

	public static final VirtualNode STATEMENTS = VirtualNode.parseOrError(0,
			Empty.class, VarAssignment.class, Expression.FunctionCall.class,
			Label.class, LoopBreak.class, Goto.class,
			DoEndBlock.class, WhileLoop.class, RepeatLoop.class, IfBlock.class,
//			CountingForLoop.class, IteratorForLoop.class,
			FunctionDeclaration.class,
			LocalVarDeclaration.class, LocalFunctionDeclaration.class);
	
	public record Empty() implements Statement {
		
		public static final Input PATTERN = Input.token(LuaToken.STATEMENT_END);
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
	}
	
	public record Label(
			@TokenValue("name") String name
	) implements Statement {
		
		public static final Input PATTERN = Input.allOf(
				Input.token(LuaToken.GOTO_LABEL),
				Input.token("name", LuaToken.NAME),
				Input.token(LuaToken.GOTO_LABEL)
				);
	}
	
	public record LoopBreak() implements Statement {
		
		public static final Input PATTERN = Input.token(LuaToken.LOOP_BREAK);
	}
	
	public record Goto(
			@TokenValue("label") String label
	) implements Statement {
		
		public static final Input PATTERN = Input.allOf(
				Input.token(LuaToken.GOTO_JUMP),
				Input.token("label", LuaToken.NAME)
				);
	}
	
	public record DoEndBlock(
			@ChildNode("block") SpecialNodes.Block block
	) implements Statement {
		
		public static final Input PATTERN = Input.allOf(
				Input.token(LuaToken.DO),
				Input.childNode("block", SpecialNodes.Block.class),
				Input.token(LuaToken.END)
				);
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
	}
	
	public record CountingForLoop(
			
	) implements Statement {
		
	}
	
	public record IteratorForLoop(
			
	) implements Statement {
		
	}
	
	public record FunctionDeclaration(
			// TODO fix function name
			@ChildNode("name") FunctionName name,
			@ChildNode("body") SpecialNodes.FunctionBody function 
	) implements Statement {
		
		public static final Input PATTERN = Input.allOf(
				Input.token(LuaToken.FUNCTION),
				Input.childNode("name", FunctionName.class),
				Input.childNode("body", SpecialNodes.FunctionBody.class)
				);
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
	}
}
