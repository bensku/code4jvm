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
		if (inputs.length == 0) {
			return;
		}
		
		// Find if stack at least one of the requested inputs
		// In case there are multiple occurrences, search for the last one
		var matchStart = stack.lastIndexOf(inputs[0]);
		
		// Count how many matches there are on stack
		var matchCount = 0;
		if (matchStart != -1) {
			for (;; matchCount++) {
				var stackSlot = matchStart + matchCount;
				if (matchCount >= inputs.length || stackSlot >= stack.size()
						|| stack.get(stackSlot) != inputs[matchCount]) {
					break;
				}
				if (inputs[matchCount] instanceof LocalVar localVar) {
					localVar.used = true;
				}
			}
			
			// Pop the values from stack, as they will be used by the expression
			// If the stack has other elements on top of them, also pop them
			for (var i = stack.size() - 1; i >= matchStart; i--) {
				stack.remove(i);
			}
		}
		
		// Mark the rest to require a local variable slot
		for (int i = matchCount; i < inputs.length; i++) {
			var value = inputs[i].original();
			if (value instanceof LocalVar localVar) {
				localVar.used = true;
				localVar.needsSlot = true;
			}
		}
	}
	
	public void checkInputs1(Value[] inputs) {
		// Find how many inputs are already loaded on stack
		var inputsStart = stack.size() - inputs.length;
		var loadedCount = 0;
		if (inputsStart > 0) {
			for (; loadedCount < inputs.length; loadedCount++) {
				var input = inputs[loadedCount];
				if (stack.get(inputsStart + loadedCount) != input) {
					break;
				}
				input = input.original();
				if (input instanceof LocalVar localVar) {
					localVar.used = true;
				}
			}
		} // else: stack has less elements than inputs, we DEFINITELY need to load the inputs
		
		// Mark the rest to require local variable slot
		for (int i = loadedCount; i < inputs.length; i++) {
			var value = inputs[i].original();
			if (value instanceof LocalVar localVar) {
				localVar.used = true;
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
		assert validateOnReset();
		stack.clear();
	}
	
	private boolean validateOnReset() {
		for (var value : stack) {
			if (value.original() instanceof LocalVar localVar) {
				// JVM stack should be clear when a block ends
				// For this reason, values that are used (i.e. not discarded)
				// and stored in stack (instead of local variables) MUST not exit
				// when the scope is reset
				// Anything else indicates a bug in our code
				assert !localVar.used || localVar.needsSlot : "stack value should have been discarded: " + value;
			}
		}
		return true;
	}

}
