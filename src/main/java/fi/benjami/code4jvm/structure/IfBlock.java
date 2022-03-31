package fi.benjami.code4jvm.structure;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import fi.benjami.code4jvm.Block;
import fi.benjami.code4jvm.Condition;
import fi.benjami.code4jvm.Statement;
import fi.benjami.code4jvm.statement.Jump;

/**
 * A block with multiple conditionally executed branches and an optional fallback.
 * 
 * <p>The branches are considered for execution in order they were added to a
 * conditional block. For each branch, this means the following:
 * 
 * <ol>
 * <li>Execute {@link Branch#test test block} associated with the branch,
 * if it exists.
 * <li>Evaluate the {@link Branch#condition condition}.
 * <li>If the condition is true, execute the {@link Branch#block main block}
 * and skip subsequent branches of the same block.
 * </ol>
 */
public class IfBlock implements Statement {
	
	/**
	 * A conditional branch.
	 */
	public record Branch(
			/**
			 * A block that is executed when this branch is considered for
			 * execution. This can be used to e.g. create the values used for
			 * {@link #condition}.
			 * 
			 * <p>Test blocks are mutated when it is added to a conditional block
			 * as part of branch. Do not reuse them elsewhere.
			 */
			Block test,
			
			/**
			 * The condition of this branch. If it is true when this branch is
			 * considered for execution (i.e. after {@link #test}), the main
			 * {@link #block} is executed.
			 */
			Condition condition,
			
			/**
			 * The main block that is executed if {@link #condition} is true
			 * immediately after {@link #test test block} (if it exists) has
			 * been executed.
			 */
			Block block
	) {}
	
	private final Block root;
	private final List<Block> edges;
	private Block fallback;
	
	private boolean emitted;
	
	public IfBlock() {
		this.root = Block.create();
		this.edges = new ArrayList<>();
	}
	
	private void ensureNotEmitted() {
		if (emitted) {
			throw new IllegalStateException("cannot modify IfBlock after it has been emitted once");
		}
	}
	
	/**
	 * Appends a branch to this conditional block.
	 * 
	 * <p>This is a low level method and you are required to construct the
	 * needed blocks yourself. Unless you have a good reason, consider using
	 * {@link #branch(Condition, Consumer)} or
	 * {@link #branch(Function, Consumer)} instead.
	 * 
	 * @param branch User-defined branch.
	 * @return This block for chaining.
	 */
	public IfBlock branch(Branch branch) {
		ensureNotEmitted();
		var edge = Block.create(); // 1 branch -> 1 edge block
		
		// Skip this branch if condition is not met
		var test = branch.test;
		var jump = Jump.to(edge, Jump.Target.END, branch.condition.not());
		if (test != null) {
			// Test block might have created values condition uses, so jump must be inside it
			test.add(jump);
			edge.add(test);
		} else {
			// No test block needed
			edge.add(jump);
		}
		
		edge.add(branch.block); // Add main block
		
		// If condition was met, rest of branches and fallback will be skipped
		// However, to avoid unnecessary jump in last branch, this is added by #emitVoid(Block)

		edges.add(edge);
		return this;
	}
	
	/**
	 * Appends a branch without {@link Branch#test test block} to this
	 * conditional block.
	 * 
	 * @param condition Condition that is checked when branch is evaluated for
	 * execution.
	 * @param callback Callback that receives the {@link Branch#block main block}.
	 * @return This block for chaining.
	 */
	public IfBlock branch(Condition condition, Consumer<Block> callback) {
		var block = Block.create();
		callback.accept(block);
		branch(new Branch(null, condition, block));
		return this;
	}
	
	/**
	 * Appends a branch with {@link Branch#test test block} to this
	 * conditional block.
	 * 
	 * @param conditionProvider Callback that receives an empty test block to
	 * be filled and returns the condition for this branch. The condition is
	 * allowed to depend on values created within the test block.
	 * @param callback Callback that receives the {@link Branch#block main block}.
	 * @return This block for chaining.
	 */
	public IfBlock branch(Function<Block, Condition> conditionProvider, Consumer<Block> callback) {
		var test = Block.create();
		var condition = conditionProvider.apply(test);
		var block = Block.create();
		callback.accept(block);
		branch(new Branch(test, condition, block));
		return this;
	}
	
	/**
	 * Sets the fallback block of this conditional block. It is executed if
	 * none of the branches are.
	 * 
	 * @param block The fallback block.
	 * @return This block for chaining.
	 */
	public IfBlock fallback(Block block) {
		ensureNotEmitted();
		if (fallback != null) {
			throw new IllegalStateException("fallback already set");
		}
		fallback = block;
		return this;
	}
	
	/**
	 * Sets the fallback block of this conditional block. It is executed if
	 * none of the branches are.
	 * 
	 * @param callback Callback that receives the empty fallback block.
	 * @return This block for chaining.
	 */
	public IfBlock fallback(Consumer<Block> callback) {
		var block = Block.create();
		callback.accept(block);
		fallback(block);
		return this;
	}

	@Override
	public void emitVoid(Block block) {
		if (emitted) {
			throw new IllegalStateException("not allowed to emit same block twice");
		}
		emitted = true;
		for (int i = 0; i < edges.size(); i++) {
			var edge = edges.get(i);
			
			// If condition was met, skip rest of branches and fallback
			if (i != edges.size() - 1 || fallback != null) {
				edge.add(Jump.to(root, Jump.Target.END));
			} // else: nothing after this, jump unnecessary
			root.add(edge);
		}
		if (fallback != null) {
			// If this is reached, everything else has been jumped over
			root.add(fallback);
		}
		block.add(root);
	}
}
