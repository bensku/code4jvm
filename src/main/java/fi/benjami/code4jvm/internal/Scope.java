package fi.benjami.code4jvm.internal;

import java.util.ArrayList;
import java.util.List;

import fi.benjami.code4jvm.Value;

public class Scope {

	private final List<Value> stack;
	
	public Scope() {
		this.stack = new ArrayList<>();
	}
	
	public void checkInputs(Value[] inputs) {
		// Find how many inputs are already loaded on stack
		var inputsStart = stack.size() - inputs.length;
		var loadedCount = 0;
		if (inputsStart > 0) {
			for (; loadedCount < inputs.length; loadedCount++) {
				if (stack.get(inputsStart + loadedCount) != inputs[loadedCount]) {
					break;
				}
			}
		} // else: stack has less elements than inputs, we DEFINITELY need to load the inputs
		
		// Mark the rest to require local variable slot
		for (int i = loadedCount; i < inputs.length; i++) {
			var value = inputs[i];
			if (value instanceof CastValue cast) {
				value = cast.original(); // Original COULD be LocalVar
			}
			if (value instanceof LocalVar localVar) {
				localVar.needsSlot = true;
			}
		}
		
		// Pop them from stack
		var firstLoaded = stack.size() - loadedCount;
		for (int i = stack.size() - 1; i >= firstLoaded; i--) {
			stack.remove(i);
		}
	}
	
	public void addOutput(Value output) {
		stack.add(output);
	}
	
	public void reset() {
		stack.clear();
	}

}
