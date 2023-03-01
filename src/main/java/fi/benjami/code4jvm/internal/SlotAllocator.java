package fi.benjami.code4jvm.internal;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

import fi.benjami.code4jvm.Type;

public class SlotAllocator {

	private int nextSlot;
	private LocalVar[] variables;
	
	public SlotAllocator() {
		this.variables = new LocalVar[8];
	}
	
	public void assignSlot(LocalVar localVar) {
		assert localVar.needsSlot; // Caller should check for needsSlot
		var slot = localVar.assignedSlot;
		if (slot == -1) {
			slot = nextSlot;
			localVar.assignedSlot = slot;
			
			if (nextSlot >= variables.length) {
				var newArray = new LocalVar[variables.length + 8];
				System.arraycopy(variables, 0, newArray, 0, variables.length);
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
	
	public int slotCount() {
		return nextSlot + 1;
	}
	
	public Stream<LocalVar> variables() {
		return Arrays.stream(variables)
				.limit(nextSlot)
				.filter(Objects::nonNull);
	}

}
