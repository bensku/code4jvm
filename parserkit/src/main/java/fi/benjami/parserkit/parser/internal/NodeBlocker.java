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
	
	private final Value topNode;
	
	public NodeBlocker(Value mask, Value topNode) {
		this.mask = mask;
		this.topNode = topNode;
	}
	
	public Value mask() {
		return mask;
	}
	
	public NodeBlocker add(Block block, int nodeId) {
		var typeMask = Constant.of(1L << nodeId);
		var newMask = block.add(BitOp.or(mask, typeMask));
		return new NodeBlocker(newMask, Constant.of(nodeId));
	}
	
	public NodeBlocker pop(Block block) {
		var shifted = block.add(BitOp.shiftLeft(Constant.of(1L), topNode));
		var typeMask = block.add(BitOp.not(shifted)); // negated mask
		var newMask = block.add(BitOp.and(mask, typeMask));
		return new NodeBlocker(newMask, topNode);
	}
	
	public Expression check(int nodeId) {
		var typeMask = Constant.of(1L << nodeId);
		return BitOp.and(mask, typeMask);
	}
	
	public Value topNode() {
		return topNode;
	}
	
//	public boolean isAlwaysBlocked(int nodeId) {
//		return topNode == nodeId;
//	}
}
