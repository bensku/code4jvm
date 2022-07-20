package fi.benjami.code4jvm.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;

import org.objectweb.asm.Opcodes;

import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.block.Method;
import fi.benjami.code4jvm.util.TypeUtils;

/**
 * Our representation of a stack map frame.
 * 
 */
public class Frame {
	
	private final BitSet slots;
	private boolean reachable, needsBytecode;
	
	private Type[] vmStack;
	
	private Frame(BitSet slots, Type[] vmStack) {
		this.slots = slots;
		this.vmStack = vmStack;
	}
	
	public Frame() {
		this.slots = new BitSet();
	}

	public void add(LocalVar localVar) {
		assert localVar.assignedSlot != -1 : "frame slot not allocated";
		slots.set(localVar.assignedSlot);
	}
	
	public boolean has(LocalVar localVar) {
		assert localVar.assignedSlot != -1 : "frame slot not allocated";
		return slots.get(localVar.assignedSlot);
	}
	
	public void setVmStack(Type[] stack) {
		vmStack = stack;
	}
	
	public Frame copy() {
		// BitSet doesn't have copy constructor :(
		return new Frame((BitSet) slots.clone(), vmStack);
	}
	
	public void mergeLeft(Frame frame) {
		if (vmStack == null) {
			vmStack = frame.vmStack;
		} else if (frame.vmStack != null && !Arrays.equals(vmStack, frame.vmStack)) {
			throw new IllegalStateException("conflicting VM-provided stack");
		}
		slots.and(frame.slots);
	}
	
	public boolean merge(Frame frame) {
		// Merge VM stack (usually empty)
		if (vmStack != null) {
			if (frame.vmStack != null) {
				throw new IllegalStateException("conflicting VM-provided stack");
			}
			frame.vmStack = vmStack;
		} else {
			vmStack = frame.vmStack;
		}
		
		// Make intersection of local variables that are available
		if (reachable) {
			// If given frame has variables available in this frame, clear them
			// If the result is equal to this frame, no variables need to be removed
			// from this frame
			frame.slots.and(slots);
			if (slots.equals(frame.slots)) {
				return false;
			} else {
				// Given frame was missing variables that are in this frame
				slots.and(frame.slots);
				return true;
			}
		} else {
			// This frame is unreachable (i.e. empty), just copy given frame
			slots.or(frame.slots);
			reachable = true;
			return true;
		}
	}
	
	public boolean isReachable() {
		return reachable;
	}
	
	public void markNeedsBytecode() {
		needsBytecode = true;
	}
	
	public boolean needsBytecode() {
		return needsBytecode;
	}
	
	public Object[] asmLocals(Method method, SlotAllocator allocator) {
		var objs = new ArrayList<>(slots.cardinality());
		
		// TODO we currently don't emit NULL, investigate if it is needed
		for (int i = 0; i < slots.length(); i++) {
			if (slots.get(i)) {
				var type = allocator.findVar(i).type();
				objs.add(toAsmFrameType(method, allocator.findVar(i).type()));
				if (type.equals(Type.LONG) || type.equals(Type.DOUBLE)) {
					i++; // Skip next slot that is part of long/double
				}
			} else {
				// When there are holes in frame, they're represented by TOP
				// (unclear why it is named like that)
				objs.add(Opcodes.TOP);
			}
		}
		return objs.toArray();
	}
	
	public Object[] asmStack(Method method) {
		if (vmStack == null) {
			return new Object[0];
		}
		return Arrays.stream(vmStack)
				.map(type -> toAsmFrameType(method, type))
				.toArray();
	}
	
	private Object toAsmFrameType(Method method, Type type) {
		if (TypeUtils.isIntLike(type) || type.equals(Type.BOOLEAN)) {
			return Opcodes.INTEGER;
		} else if (type.equals(Type.FLOAT)) {
			return Opcodes.FLOAT;
		} else if (type.equals(Type.LONG)) {
			return Opcodes.LONG;
		} else if (type.equals(Type.DOUBLE)) {
			return Opcodes.DOUBLE;
		} else if (type.equals(Type.METHOD_RETURN_TYPE)) {
			return toAsmFrameType(null, method.returnType());
		} else {
			return type.internalName(); // Reference type
		}
	}
	
	@Override
	public boolean equals(Object o) {
		return o instanceof Frame frame && slots.equals(frame.slots);
	}
}
