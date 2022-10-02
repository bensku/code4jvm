package fi.benjami.code4jvm.block;

import java.util.Map;
import java.util.stream.Collectors;

import fi.benjami.code4jvm.block.Block.Backlinks;
import fi.benjami.code4jvm.internal.DebugNames;
import fi.benjami.code4jvm.internal.LocalVar;
import fi.benjami.code4jvm.internal.node.EdgeNode;

/**
 * Helper for recursively printing contents of a block and its sub-blocks.
 * 
 * @see Block#toString(fi.benjami.code4jvm.internal.DebugNames.Counting, boolean, String)
 *
 */
public record BlockPrinter(
		/**
		 * Debug name generator for unnamed {@link LocalVar local variables}.
		 */
		DebugNames.Counting localNameGen,
		
		/**
		 * Name generator for all {@link Block blocks}.
		 */
		DebugNames.ForBlocks blockNameGen,
		
		/**
		 * Name generator for all {@link EdgeNode edges}.
		 */
		DebugNames.Counting edgeNameGen,
		
		/**
		 * Blocks mapped to edges which jump into their start/end.
		 * Null if this information is not available; in this case, we just
		 * don't print incoming jumps.
		 * 
		 * @see Block#findBacklinks
		 */
		Map<Block, Backlinks> backlinks,
		StringBuilder sb
) {
	
	public BlockPrinter(DebugNames.Counting localNameGen, Map<Block, Backlinks> backlinks) {
		this(localNameGen, new DebugNames.ForBlocks(), new DebugNames.Counting("edge_"),
				backlinks, new StringBuilder());
	}
	
	public void append(Block block, String indent) {
		sb.append(indent).append(blockNameGen.make(block, block.debugName)).append(" {\n");
		var newIndent = indent + "    ";
		
		if (backlinks != null) {
			sb.append(printBacklinks(newIndent, backlinks.get(block), true));
		}
		
		for (var node : block.nodes) {
			if (node instanceof EdgeNode edge) {
				var type = edge.type();
				if (type == EdgeNode.SUB_BLOCK) {
					// Recursively print sub-blocks
					append(edge.target(), newIndent);
				} else if (type == EdgeNode.UNCONDITIONAL_JUMP || type == EdgeNode.CONDITIONAL_JUMP) {
					// Print which block the jump leads to (and start/end)
					sb.append(newIndent).append("OUTGOING JUMP ")
							.append(edgeNameGen.make(edge)).append(" -> (")
							.append(blockNameGen.make(block, edge.target().debugName))
							.append(", ").append(edge.position()).append(")\n");
				} else if (type == EdgeNode.RETURN) {
					// Print where the return leads to (out of method or redirected to block)
					sb.append(newIndent);
					var redirect = block.returnRedirect();
					if (redirect != null) {
						sb.append("RETURN (redirect to ")
								.append(blockNameGen.make(block, redirect.target().debugName));
						redirect.valueHolder().ifPresentOrElse(value -> {
							sb.append(", value saved in ").append(((LocalVar) value).toString(localNameGen));
						}, () -> {
							sb.append(", value discarded");
						});
						sb.append(")\n");
					} else {
						sb.append("RETURN (no redirect)\n");
					}
				} // Ignore THROW, exception handlers could be in other methods
			} else {
				// Print other nodes using toString (but provide LocalVar name generator)
				sb.append(newIndent).append(node.toString(localNameGen)).append("\n");
			}
		}
		if (backlinks != null) {
			sb.append(printBacklinks(newIndent, backlinks.get(block), false));
		}
		
		sb.append(indent).append("}\n");
	}
	
	private String printBacklinks(String indent, Backlinks links, boolean start) {
		if (links == null) {
			return "";
		}
		var sources = start ? links.toStart() : links.toEnd();
		if (sources.isEmpty()) {
			return "";
		}
		return indent + "INCOMING JUMPS: " + sources.stream()
				.map(edge -> edgeNameGen.make(edge))
				.collect(Collectors.joining(", ")) + "\n";
	}
	
	@Override
	public String toString() {
		return sb.toString();
	}
}
