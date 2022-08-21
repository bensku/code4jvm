package fi.benjami.code4jvm.internal;

import fi.benjami.code4jvm.Type;

public class SlotAllocator {

	private int nextSlot;
	private LocalVar[] variables;
	
	public SlotAllocator(SlotAllocator parent) {
		if (parent != null) {
			this.nextSlot = parent.nextSlot;
			this.variables = new LocalVar[parent.variables.length + 8];
			System.arraycopy(parent.variables, 0, variables, 0, parent.nextSlot);
		} else {
			this.variables = new LocalVar[8];
		}
	}
	
	public void assignSlot(LocalVar localVar) {
		var slot = localVar.assignedSlot;
		if (slot == -1) {
			slot = nextSlot;
			localVar.assignedSlot = slot;
			
			if (nextSlot >= variables.length) {
				var newArray = new LocalVar[variables.length + 8];
				System.arraycopy(variables, 0, newArray, 0, nextSlot);
				variables = newArray;
			}
			variables[nextSlot] = localVar;
			
			var type = localVar.type();
			if (type == Type.LONG || type == Type.DOUBLE) {
				nextSlot += 2;
			} else {
				nextSlot += 1;
			}
		}
	}
	
	public LocalVar findVar(int slot) {
		return variables[slot];
	}

}
