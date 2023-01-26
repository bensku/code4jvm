package fi.benjami.code4jvm.internal;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import fi.benjami.code4jvm.Type;

public class SlotAllocator {

	private int nextSlot;
	private LocalVar[] variables;
	
	/**
	 * Slot count of this + argument slots.
	 */
	private int argSlotCount;
	
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
	
	public void assignArgSlots(List<LocalVar> args) {
		for (var localVar : args) {
			assignSlot(localVar);
		}
		argSlotCount = nextSlot;
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
	
	public boolean isMethodArg(LocalVar localVar) {
		return localVar.assignedSlot != -1 && localVar.assignedSlot < argSlotCount;
	}

}
