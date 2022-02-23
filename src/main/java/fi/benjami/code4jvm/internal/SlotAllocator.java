package fi.benjami.code4jvm.internal;

import org.objectweb.asm.Type;

public class SlotAllocator {

	private int nextSlot;
	
	public SlotAllocator(SlotAllocator parent) {
		if (parent != null) {
			this.nextSlot = parent.nextSlot;
		}
	}
	
	public int get(LocalVar localVar) {
		var slot = localVar.assignedSlot;
		if (slot == -1) {
			slot = nextSlot;
			localVar.assignedSlot = slot;
			var type = localVar.type();
			if (type == Type.LONG_TYPE || type == Type.DOUBLE_TYPE) {
				nextSlot += 2;
			} else {
				nextSlot += 1;
			}
		}
		return slot;
	}
}
