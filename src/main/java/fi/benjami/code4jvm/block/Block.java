package fi.benjami.code4jvm.block;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.Label;

import fi.benjami.code4jvm.CompileHook;
import fi.benjami.code4jvm.Expression;
import fi.benjami.code4jvm.Statement;
import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.Value;
import fi.benjami.code4jvm.Variable;
import fi.benjami.code4jvm.internal.DebugNames;
import fi.benjami.code4jvm.internal.Frame;
import fi.benjami.code4jvm.internal.LocalVar;
import fi.benjami.code4jvm.internal.MethodCompilerState;
import fi.benjami.code4jvm.internal.Scope;
import fi.benjami.code4jvm.internal.node.CodeNode;
import fi.benjami.code4jvm.internal.node.EdgeNode;
import fi.benjami.code4jvm.internal.node.Node;
import fi.benjami.code4jvm.internal.node.StoreNode;
import fi.benjami.code4jvm.internal.node.VarMarkerNode;
import fi.benjami.code4jvm.statement.Bytecode;
import fi.benjami.code4jvm.statement.Jump;
import fi.benjami.code4jvm.statement.Return;
import fi.benjami.code4jvm.statement.Throw;
import fi.benjami.code4jvm.typedef.ClassDef;

public class Block implements CompileHook.Carrier {
	
	public static Block create() {
		return create("unnamed");
	}
	
	public static Block create(String debugName) {
		return new Block(new ArrayList<>(), new Scope(), debugName);
	}
	
	public record Edge(
			Block target,
			Jump.Target position,
			boolean conditional,
			Type[] vmStack
	) {}
	
	final List<Node> nodes;
	final Scope scope;
	final String debugName;
	
	private Map<Object, CompileHook> hooks;
	private Label startLabel, endLabel;	
	private ReturnRedirect returnRedirect;
	
	Block parent;
	int parentNodeIndex;
	
	final BitSet reachability;
	final Frame startFrame;
	final Frame endFrame;
	
	private Block(ArrayList<Node> nodes, Scope scope, String debugName) {
		this.nodes = nodes;
		this.scope = scope;
		this.debugName = debugName;
		this.reachability = new BitSet();
		this.startFrame = new Frame();
		this.endFrame = new Frame();
	}
	
	public void add(Statement stmt) {
		if (stmt instanceof Return ret) {
			stmt.emitVoid(this);
			// Returns are edges - they lead out of method or jump to redirect
			nodes.add(new EdgeNode(null, Jump.Target.START, EdgeNode.RETURN, null));
		} else if (stmt instanceof Throw thr) {
			stmt.emitVoid(this);
			nodes.add(new EdgeNode(null, Jump.Target.START, EdgeNode.THROW, null));
		} else if (stmt instanceof StoreNode store) {
			scope.checkInputs(new Value[] {store.value()}, true);
			nodes.add(store);
		} else {
			stmt.emitVoid(this);
		}
	}
	
	public Value add(Expression expr) {
		if (expr instanceof Bytecode bc) {
			scope.checkInputs(bc.inputs(), (bc.flags() & Bytecode.EXPLICIT_LOAD) == 0);
			
			var output = new LocalVar(bc.outputType(), null);
			var node = new CodeNode(bc, output);
			nodes.add(node);
			return output;
		} else {
			return expr.emitValue(this);
		}
	}
	
	public Variable add(Variable output, Expression expr) {
		nodes.add(new VarMarkerNode(true, (LocalVar) output));
		var value = add(expr);
		add(output.set(value));
		nodes.add(new VarMarkerNode(false, (LocalVar) output));
		return output;
	}
	
	public Variable add(String outputName, Expression expr) {
		// Insert the marker without local variable, because we need it before
		// initializer of the local variable, but it needs the type returned by
		// the initializer
		var firstMarker = new VarMarkerNode(true, null);
		nodes.add(firstMarker);
		var value = add(expr);
		var output = Variable.create(value.type(), outputName);
		firstMarker.localVar = (LocalVar) output; // Fill in the local variable
		add(output.set(value));
		nodes.add(new VarMarkerNode(false, (LocalVar) output));
		return output;
	}
	
	public void add(Block block) {
		if (block.parent != null) {
			throw new IllegalArgumentException("block cannot be added twice; try copy() instead");
		}
		block.parent = this;
		block.parentNodeIndex = nodes.size();
		
		var edge = new EdgeNode(block, Jump.Target.START, EdgeNode.SUB_BLOCK, null);
		nodes.add(edge);
		// Reset scope, stack will be gone after the newly added block
		scope.resetStack();
	}
	
	public void patchToStart(Block block) {
		if (block.parent != null) {
			throw new IllegalArgumentException("block cannot be added twice; try copy() instead");
		}
		block.parent = this;
		block.parentNodeIndex = 0;
		
		var edge = new EdgeNode(block, Jump.Target.START, EdgeNode.SUB_BLOCK, null);
		nodes.add(0, edge);
	}
	
	public Label add(Edge edge) {
		var targetLabel = edge.target().requestLabel(edge.position());
		// SUB_BLOCK and RETURN are only for internal usage
		nodes.add(new EdgeNode(edge.target(), edge.position(), edge.conditional()
				? EdgeNode.CONDITIONAL_JUMP : EdgeNode.UNCONDITIONAL_JUMP, edge.vmStack()));
		return targetLabel;
	}
	
	// TODO consider if this foot-gun should be public API
	public Label requestLabel(Jump.Target position) {
		return switch (position) {
		case START -> {
			if (startLabel == null) {
				startLabel = new Label();
			}
			yield startLabel;
		}
		case END -> {
			if (endLabel == null) {
				endLabel = new Label();
			}
			yield endLabel;
		}
		};
	}
	
	/**
	 * Sets a compile hook to this block. It is called when this block is
	 * compiled as part of a {@link ClassDef class definition}.
	 * 
	 * <p>Compile hooks may be called for multiple times for same class and
	 * should not e.g. generate duplicate methods when this occurs.
	 * @param hook The hook.
	 */
	public void setCompileHook(Object key, CompileHook hook) {
		if (hooks == null) {
			hooks = new IdentityHashMap<>(1);
		}
		hooks.put(key, hook);
	}
	
	/**
	 * Sets a return redirect for this block and its sub-blocks.
	 * 
	 * <p>All {@link Return returns} will be replaced with jumps to
	 * {@link ReturnRedirect#target() redirect target}, and the value that
	 * would have been returned is stored in
	 * {@link ReturnRedirect#valueHolder() holder variable}. This is a good
	 * use-case for {@link Variable#create(Type) unbound variables}.
	 * @param redirect Return redirect.
	 */
	public void setReturnRedirect(ReturnRedirect redirect) {
		returnRedirect = redirect;
	}
	
	public ReturnRedirect returnRedirect() {
		return returnRedirect;
	}
		
	void emitBytecode(MethodCompilerState state) {
		var ctx = state.ctx();
		state.frames().visitFrame(startFrame);
		if (startLabel != null) {
			ctx.asm().visitLabel(startLabel);
		}
		
		// Call compile hooks (used by e.g. lambda method generation)
		if (hooks != null) {			
			for (var hook : hooks.values()) {
				hook.onCompile(ctx.owner());
			}
		}
		
		// Ask nodes to emit bytecode (but skip dead code)
		for (var i = 0; i < nodes.size(); i++) {
			var reachable = reachability.get(i);
			var node = nodes.get(i);
			if (node instanceof EdgeNode edge) {
				// Sub-blocks may get jumped into, for which reason we need to
				// ALWAYS emit them even if they're not directly reachable
				// No bytecode is emitted if block is actually unreachable
				if (edge.type() == EdgeNode.SUB_BLOCK) {
					edge.target().emitBytecode(state);
				}
			} else if (node instanceof CodeNode codeNode && reachable) {
				state.frames().visitCode(ctx.asm());
				codeNode.emitBytecode(state, this);
				ctx.stack().releaseStack(codeNode.outputType(), codeNode.outputToStack());
			} else if (node instanceof StoreNode storeNode && reachable) {
				state.frames().visitCode(ctx.asm());
				storeNode.emitBytecode(state);
				ctx.stack().releaseStack(Type.VOID, false);
			} else if (node instanceof VarMarkerNode markerNode && state.emitVarMarkers()) {
				markerNode.visitLabels(ctx.asm());
			}
		}
		if (endLabel != null) {
			ctx.asm().visitLabel(endLabel);
		}
		state.frames().visitFrame(endFrame);
	}
	
	record Backlinks(Set<EdgeNode> toStart, Set<EdgeNode> toEnd) {
		public Backlinks() {
			this(Collections.newSetFromMap(new IdentityHashMap<>()), Collections.newSetFromMap(new IdentityHashMap<>()));
		}
	}
	
	/**
	 * Recursively traverses all sub-blocks to compute where links lead to.
	 * @param backlinks Map to fill with backlinks.
	 * @param block Block to process.
	 */
	private static void findBacklinks(Map<Block, Backlinks> backlinks, Block block) {
		for (var node : block.nodes) {
			if (node instanceof EdgeNode edge) {
				var links = backlinks.computeIfAbsent(edge.target(), k -> new Backlinks());
				if (edge.position() == Jump.Target.START) {
					links.toStart().add(edge);
				} else {
					links.toEnd().add(edge);
				}
				
				if (edge.type() == EdgeNode.SUB_BLOCK) {
					findBacklinks(backlinks, edge.target());
				}
			}
		}
	}
	
	@Override
	public String toString() {
		return toString(new DebugNames.Counting("var_"), false, "");
	}
	
	String toString(DebugNames.Counting localNameGen, boolean computeBacklinks, String indent) {
		Map<Block, Backlinks> backlinks = null;
		if (computeBacklinks) {
			backlinks = new HashMap<>();
			findBacklinks(backlinks, this);
		}
		var printer = new BlockPrinter(localNameGen, backlinks);
		printer.append(this, indent);
		return printer.toString();
	}
			
}
