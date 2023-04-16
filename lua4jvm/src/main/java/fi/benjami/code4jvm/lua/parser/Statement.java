package fi.benjami.code4jvm.lua.parser;

import java.util.List;

import fi.benjami.code4jvm.lua.parser.SpecialNodes.Block;
import fi.benjami.code4jvm.lua.parser.SpecialNodes.FunctionBody;
import fi.benjami.parserkit.parser.VirtualNode;
import fi.benjami.parserkit.parser.ast.ChildNode;
import fi.benjami.parserkit.parser.ast.TokenValue;

public interface Statement extends LuaNode {

	public static final VirtualNode STATEMENTS = VirtualNode.parseOrError(0,
			Empty.class, VarAssignment.class, Expression.FunctionCall.class,
			Label.class, LoopBreak.class, Goto.class,
			DoEndBlock.class, WhileLoop.class, RepeatLoop.class, IfBlock.class,
			CountingForLoop.class, IteratorForLoop.class,
			FunctionDeclaration.class,
			LocalVarDeclaration.class, LocalFunctionDeclaration.class);
	
	public record Empty() implements Statement {
		
	}
	
	public record VarAssignment(
			@ChildNode("variables") List<Expression.VarReference> variables,
			@ChildNode("values") List<Expression> values
	) implements Statement {
		
	}
	
	public record LocalVarDeclaration(
			@TokenValue("varNames") List<String> varNames
	) implements Statement {
		
	}
	
	public record Label(
			@TokenValue("name") String name
	) implements Statement {
		
	}
	
	public record LoopBreak() implements Statement {
		
	}
	
	public record Goto(
			@TokenValue("label") String label
	) implements Statement {
		
	}
	
	public record DoEndBlock(
			@ChildNode("block") Block block
	) implements Statement {
		
	}
	
	public record WhileLoop(
			
	) implements Statement {
		
	}
	
	public record RepeatLoop(
			
	) implements Statement {
		
	}
	
	public record IfBlock(
			
	) implements Statement {
		
	}
	
	public record CountingForLoop(
			
	) implements Statement {
		
	}
	
	public record IteratorForLoop(
			
	) implements Statement {
		
	}
	
	public record FunctionDeclaration(
			// TODO fix function name
			@TokenValue("name") String name,
			@ChildNode("body") FunctionBody function 
	) implements Statement {
		
	}
	
	public record LocalFunctionDeclaration(
			@TokenValue("name") String name,
			@ChildNode("body") FunctionBody function
	) implements Statement {
		
	}
	
	// Official grammar states that return is not a statement, but this
	// is probably best detected during semantic analysis
	public record Return(
			@ChildNode("values") List<Expression> values
	) {
		
	}
}
