package fi.benjami.code4jvm.internal.node;

import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.block.Block;
import fi.benjami.code4jvm.statement.Jump;

/**
 * Node that represents a block edge for control flow analysis.
 * 
 * @see Block.Edge
 *
 */
public record EdgeNode(
		/**
		 * Block the edge leads to. Null if type is RETURN, because parent
		 * block sets it with return redirects.
		 */
		Block target,
		
		/**
		 * Where at block the edge leads to.
		 */
		Jump.Target position,
		
		/**
		 * Type of this block (see ints below).
		 */
		int type,
		
		/**
		 * Stack provided by JVM at target.
		 */
		Type[] vmStack
) implements Node {
	
	public static final int SUB_BLOCK = 1, CONDITIONAL_JUMP = 2, UNCONDITIONAL_JUMP = 3, RETURN = 4, THROW = 5;
	
	public EdgeNode {
		assert (target == null) == (type == RETURN || type == THROW);
		assert position == Jump.Target.START || (type == CONDITIONAL_JUMP || type == UNCONDITIONAL_JUMP);
		assert vmStack == null || type != SUB_BLOCK;
	}
}
