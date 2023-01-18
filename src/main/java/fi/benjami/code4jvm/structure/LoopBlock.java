package fi.benjami.code4jvm.structure;

import fi.benjami.code4jvm.Condition;
import fi.benjami.code4jvm.Statement;
import fi.benjami.code4jvm.block.Block;
import fi.benjami.code4jvm.statement.Jump;

/**
 * A block that repeatedly executes another block of code. Both
 * {@link #whileLoop(Block, Condition) 'while'} and
 * {@link #doWhileLoop(Block, Condition) 'do-while} styles of blocks are
 * supported.
 * 
 * <p>To create a Java-style for loop, declare a counter outside of the
 * looped block and mutate it inside of the block.
 *
 */
public class LoopBlock implements Statement {

	/**
	 * Creates a new loop block.
	 * @param body Loop body.
	 * @param condition Condition to check before/after each iteration.
	 * @param conditionBefore Whether or not the condition should be checked
	 * before ('do-while') or after the loop body ('while').
	 * @return
	 */
	public static LoopBlock of(Block body, Condition condition, boolean conditionBefore) {
		return new LoopBlock(body, condition, conditionBefore);
	}
	
	public static LoopBlock whileLoop(Block body, Condition condition) {
		return of(body, condition, true);
	}
	
	public static LoopBlock doWhileLoop(Block body, Condition condition) {
		return of(body, condition, false);
	}
	
	private final Block parent;
	
	private LoopBlock(Block body, Condition condition, boolean conditionBefore) {
		this.parent = createParent(body, condition, conditionBefore);
	}
	
	private Block createParent(Block body, Condition condition, boolean conditionBefore) {
		var parent = Block.create("loop parent");
		if (conditionBefore) {
			// If condition is not met, skip the body and end loop
			parent.add(Jump.to(parent, Jump.Target.END, condition.not()));
		}
		parent.add(body);
		if (!conditionBefore) {
			// If condition is met, jump back to start
			parent.add(Jump.to(parent, Jump.Target.START, condition));
		} else {
			// Condition is already checked at start, just unconditionally jump there
			parent.add(Jump.to(parent, Jump.Target.START));
		}
		return parent;
	}
	
	/**
	 * Creates a statement that breaks out of this loop.
	 * @return A break statement.
	 */
	public Statement breakStmt() {
		return (block) -> {
			block.add(Jump.to(parent, Jump.Target.END));
		};
	}
	
	/**
	 * Creates a statement that continues directly into next iteration of
	 * this loop.
	 * @return A continue statement.
	 */
	public Statement continueStmt() {
		return (block) -> {
			block.add(Jump.to(parent, Jump.Target.START));
		};
	}
	
	@Override
	public void emitVoid(Block block) {
		block.add(parent);
	}

}
