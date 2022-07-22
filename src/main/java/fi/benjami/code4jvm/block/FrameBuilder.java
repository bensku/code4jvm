package fi.benjami.code4jvm.block;

import fi.benjami.code4jvm.UninitializedValueException;
import fi.benjami.code4jvm.internal.Frame;
import fi.benjami.code4jvm.internal.LocalVar;
import fi.benjami.code4jvm.internal.SlotAllocator;
import fi.benjami.code4jvm.internal.node.CodeNode;
import fi.benjami.code4jvm.internal.node.EdgeNode;
import fi.benjami.code4jvm.internal.node.StoreNode;
import fi.benjami.code4jvm.statement.Jump;

/**
 * A tool for building JVM stack map frames and validating that values are not
 * used before they are defined.
 */
class FrameBuilder {
	
	private final SlotAllocator allocator;
	private boolean allowMutations;
	
	public FrameBuilder(SlotAllocator allocator) {
		this.allocator = allocator;
		this.allowMutations = true;
	}
	
	/**
	 * Builds {@link Frame frames} for all blocks within given method. This
	 * is done by recursively tracing the control flow of the method. The
	 * built frames are attached to blocks ({@link Block#startFrame} and
	 * {@link Block#endFrame}) and sub-block {@link EdgeNode edges}.
	 * 
	 * <p>In addition to building frames required by the JVM, this throws
	 * {@link UninitializedValueException} if values are used before they were
	 * defined.
	 * @param method Method to trace.
	 */
	public void trace(ConcreteMethod method) {
		var root = method.block();
		var rootFrame = new Frame();
		// 'this'/self and method arguments are visible to entire method
		if (method instanceof Method.Instance instance) {
			rootFrame.add(instance.self);
		}
		for (var arg : method.args) {
			rootFrame.add(arg);
		}
		
		trace(root, rootFrame, 0, false);
		// If assertions are enabled, make sure that the frames don't change when recomputed
		// This could help catch bugs in tracing code
		assert framesAreStable(root, rootFrame);
	}
	
	private boolean framesAreStable(Block block, Frame frame) {
		try {
			allowMutations = false;
			trace(block, frame, 0, false);
		} finally {
			allowMutations = true;
		}
		return true; // Would have failed assert in trace(...)
	}
	
	private void trace(Block block, Frame frame, int startNode, boolean jump) {
		assert startNode == 0 || startNode == block.nodes.size()
				|| block.nodes.get(startNode - 1) instanceof EdgeNode
				: "tracing can only start from edges";
		
		if (startNode == 0) {
			var modified = block.startFrame.merge(frame);
			assert allowMutations || !modified;
			if (jump) {
				block.startFrame.markNeedsBytecode();
			}
			if (!modified) {
				// Start frame was not modified, no need to trace again
				return;
			}
		} else if (startNode < block.nodes.size()) {
			assert !jump; // This should be sub-block returning control to us
			var edge = (EdgeNode) block.nodes.get(startNode - 1);
			var modified = block.subBlockFrames.get(edge).merge(frame);
			assert allowMutations || !modified;
		}
		
		if (block.returnRedirect() == null && block.parent != null) {
			// Inherit ReturnRedirect from this block if it is not set
			block.setReturnRedirect(block.parent.returnRedirect());
		}
		
		// Since jumps can only enter from start or end, start frame must have
		// been visited unless this was entered from end
		assert block.startFrame.isReachable() || startNode == block.nodes.size();
		
		for (int i = startNode; i < block.nodes.size(); i++) {
			block.reachability.set(i); // Tracing control flow got us here, so node is reachable
			var node = block.nodes.get(i);
			if (node instanceof EdgeNode edge) {
				var type = edge.type();
				if (type == EdgeNode.UNCONDITIONAL_JUMP) {
					// The node after edge has code for jumping
					// TODO this is kind of a hack, better ideas?
					block.reachability.set(i + 1);
				}
				
				// TODO do we need to copy for anything but conditional jumps?
				var snapshot = frame.copy();
				
				// If target has VM-provided stack, add it to frame
				var vmStack = edge.vmStack();
				if (vmStack != null && vmStack.length != 0) {
					snapshot.setVmStack(vmStack);
				}
				
				if (type == EdgeNode.RETURN) {
					// Return is jump if parent block has set up return redirect
					var redirect = block.returnRedirect();
					if (redirect != null) {
						// Add ASM label to redirect now that we know where it leads to
						// This allows Return to get in in bytecode generation phase
						redirect.target().requestLabel(Jump.Target.START);
						
						// Return redirect can see what was visible at start of this block
						// Additionally, if the return value is captured to uninitialized variable
						// it also becomes available
						var redirectFrame = block.startFrame.copy();
						var holder = redirect.valueHolder().orElse(null);
						if (holder != null) {
							// This doesn't use the normal Scope system, so we'll have to
							// make sure it has a slot manually
							var localVar = (LocalVar) holder.original();
							localVar.needsSlot = true;
							allocator.get(localVar);
							redirectFrame.add(localVar);
						}
						trace(redirect.target(), redirectFrame, 0, true);
					}
				} else if (type == EdgeNode.THROW) {
					// No-op
				} else {
					// Sub-block or jump
					if (edge.position() == Jump.Target.START) {
						// Trace the target block from start to end
						trace(edge.target(), snapshot, 0, type != EdgeNode.SUB_BLOCK);
					} else {
						// END is always a jump, never sub-block
						trace(edge.target(), snapshot, edge.target().nodes.size(), true);
					}
				}
				
				if (type != EdgeNode.CONDITIONAL_JUMP) {
					// Although sub-blocks may return control to their parent,
					// they modify the frame before
					// We'll need to process them recursively like jumps
					return;
				}
			} else if (node instanceof CodeNode code) {
				// Verify that inputs are available
				code.validateInputs(allocator, frame, block);
				// Add output to frame if it needs a local variable slot
				code.addOutput(allocator, frame);
				
				frame.setVmStack(null);
			} else if (node instanceof StoreNode store) {
				// Validate that whatever we're storing is available
				if (store.value().original() instanceof LocalVar localVar && localVar.needsSlot) {
					allocator.get(localVar); // Make sure slot is available
					if (!frame.has(localVar)) {
						throw new UninitializedValueException(localVar, block);
					}
				}
				if (!store.target().startInitialized) {
					// Variables that start uninitialized are added to frame
					// only when something is stored to them
					allocator.get(store.target());
					frame.add(store.target());
				}
				
				// For simplicity, the first node MUST consume VM stack
				// by e.g. setting it to a variable tracked by us
				frame.setVmStack(null);
			}
		}
		
		// Reached the end, somehow
		var modified = block.endFrame.merge(frame);
		assert allowMutations || !modified;
		if (jump) {
			block.endFrame.markNeedsBytecode();
		}
		if (modified && block.parent != null) {
			// Blocks have implicit edge that leads to their parent block
			// This is done so that sub-blocks can make new variables visible
			// to their parent blocks
			trace(block.parent, frame, block.parentNodeIndex + 1, false);
		}
	}

}
