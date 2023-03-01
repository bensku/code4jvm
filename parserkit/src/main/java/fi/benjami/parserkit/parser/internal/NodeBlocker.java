package fi.benjami.parserkit.parser.internal;

import fi.benjami.code4jvm.Constant;
import fi.benjami.code4jvm.Expression;
import fi.benjami.code4jvm.Value;
import fi.benjami.code4jvm.block.Block;
import fi.benjami.code4jvm.statement.BitOp;

/**
 * Blocks left recursion.
 *
 */
public class NodeBlocker {

	/**
	 * Currently disallowed AST nodes in a bitfield.
	 */
	private final Value mask;
	
	/**
	 * Id of node that was given to {@link #add(Block, int)} call that
	 * created this blocker. If this was created using some other way,
	 * this is -1.
	 */
	private final int topNode;
	
	public NodeBlocker(Value mask) {
		this(mask, -1);
	}
	
	private NodeBlocker(Value mask, int topNode) {
		this.mask = mask;
		this.topNode = topNode;
	}
	
	public Value mask() {
		return mask;
	}
	
	public NodeBlocker add(Block block, int nodeId) {
		var typeMask = Constant.of(1L << nodeId);
		var newMask =  block.add(BitOp.or(mask, typeMask));
		return new NodeBlocker(newMask, nodeId);
	}
	
	public NodeBlocker pop(Block block) {
		if (topNode == -1) {
			return this;
		}
		var typeMask = Constant.of(~(1L << topNode)); // negated mask
		var newMask = block.add(BitOp.and(mask, typeMask));
		return new NodeBlocker(newMask);
	}
	
	public Expression check(int nodeId) {
		var typeMask = Constant.of(1L << nodeId);
		return BitOp.and(mask, typeMask);
	}
}
