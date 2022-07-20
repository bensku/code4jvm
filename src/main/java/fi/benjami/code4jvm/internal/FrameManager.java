package fi.benjami.code4jvm.internal;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import fi.benjami.code4jvm.block.Method;

/**
 * Support code for frames JVM needs to bytecode.
 *
 */
public class FrameManager {

	private final Method method;
	private final SlotAllocator allocator;
	
	private Frame previous;
	
	public FrameManager(Method method, SlotAllocator allocator) {
		this.method = method;
		this.allocator = allocator;
	}
	
	/**
	 * Emits a frame that is intersection of all
	 * {@link #visitFrame(Frame) visited frames} after this method was
	 * previously called
	 * @param mv ASM visitor.
	 */
	public void visitCode(MethodVisitor mv) {
		if (previous != null && previous.needsBytecode()) {
			var asmLocals = previous.asmLocals(method, allocator);
			// We don't allow stack values at start of blocks
			// Unfortunately, JVM creates them for us in e.g. exception handlers
			var asmStack = previous.asmStack(method);
			// ASM will compress the frames
			mv.visitFrame(Opcodes.F_NEW, asmLocals.length, asmLocals, asmStack.length, asmStack);
		}
		previous = null;
	}
	
	/**
	 * Visits a frame for future {@link #visitCode(MethodVisitor)} usage.
	 * @param frame Frame to visit.
	 */
	public void visitFrame(Frame frame) {
		if (!frame.isReachable()) {
			// Unreachable frames have NO variables available
			// Merging with one would result in an empty frame, so let's not do that
			return;
		}
		
		if (previous != null) {
			// Note: we can't modify frame given to us, so no normal merging here!
			previous.mergeLeft(frame);
		} else {
			previous = frame.copy(); // previous will be mutated, so need to copy
		}
		if (frame.needsBytecode()) {
			// This frame may never be emitted, but the previous one will be
			previous.markNeedsBytecode();
		}
	}
}
