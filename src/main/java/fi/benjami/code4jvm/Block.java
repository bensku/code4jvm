package fi.benjami.code4jvm;

import java.util.ArrayList;
import java.util.List;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import fi.benjami.code4jvm.internal.BlockNode;
import fi.benjami.code4jvm.internal.CodeNode;
import fi.benjami.code4jvm.internal.CompileContext;
import fi.benjami.code4jvm.internal.LocalVar;
import fi.benjami.code4jvm.internal.NeedsBlockLabels;
import fi.benjami.code4jvm.internal.Node;
import fi.benjami.code4jvm.internal.Scope;
import fi.benjami.code4jvm.statement.Bytecode;

public class Block {
	
	public static Block create() {
		return new Block();
	}

	public class AddExpression {
		
		private final Value value;
		private final CodeNode node;
		
		AddExpression(Value value, CodeNode node) {
			this.value = value;
			this.node = node;
		}
		
		public Value value(String name) {
			if (value instanceof Constant) {
				// Constants can be used as immutable values as-is
				return value;
			}
			return variable(name);
		}
		
		public Value value() {
			return value(null);
		}
		
		public Variable variable(String name) {
			if (value instanceof LocalVar localVar) {				
				// Assign name to the underlying value
				if (localVar.name().isPresent()) {
					throw new AssertionError("internal code should not assign names");
				}
				if (name != null) {
					localVar.name(name);
				}
				
				if (node != null) {
					// This is Bytecode expression, nothing else has Node
					// Assign our variable to node to not discard it during bytecode generation
					node.assignVar(localVar);
					// Keep track of stack
					scope.addOutput(localVar);
				}
				return localVar;
			} else {
				throw new UnsupportedOperationException("constant -> var");
			}
		}
		
		public Variable variable() {
			return variable(null);
		}
	}
	
	private final List<Node> nodes;
	private final Scope scope;
	
	private Block parent;
	private Label startLabel, endLabel;
	
	Block() {
		this.nodes = new ArrayList<>();
		this.scope = new Scope();
	}
	
	public void add(Statement stmt) {
		if (stmt instanceof NeedsBlockLabels labelConsumer) {
			// Note that the jump (or whatever else) could lead to another block!
			var block = labelConsumer.targetBlock();
			var start = block.startLabel != null ? block.startLabel : new Label();
			var end = block.endLabel != null ? block.endLabel : new Label();
			var needs = labelConsumer.setLabels(start, end);
			
			// Make sure that the needed labels are actually given to ASM
			if ((needs & NeedsBlockLabels.NEED_START) != 0) {
				block.startLabel = start;
			}
			if ((needs & NeedsBlockLabels.NEED_END) != 0) {
				block.endLabel = end;
			}
		}
		stmt.emitVoid(this);
	}
	
	public AddExpression add(Expression expr) {
		if (expr instanceof Bytecode bc) {
			scope.checkInputs(bc.inputs());
			// Output is added in AddExpression if it is called
			
			var node = new CodeNode(bc);
			var tempValue = new LocalVar(bc.outputType(), this);
			nodes.add(node);
			return new AddExpression(tempValue, node);
		} else {
			return new AddExpression(expr.emitValue(this), null);
		}
	}
	
	public void add(Block block) {
		block.parent = this;
		nodes.add(new BlockNode(block));
		// Reset scope, stack will be gone after the newly added block
		scope.reset();
	}
	
	void emitBytecode(CompileContext ctx) {
		if (startLabel != null) {
			ctx.asm().visitLabel(startLabel);
		}
		for (var node : nodes) {
			if (node instanceof BlockNode blockNode) {
				blockNode.block().emitBytecode(ctx);
			} else if (node instanceof CodeNode codeNode) {
				codeNode.emitBytecode(ctx);
			}
		}
		if (endLabel != null) {
			ctx.asm().visitLabel(endLabel);
		}
	}
	
}
