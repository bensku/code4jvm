package fi.benjami.code4jvm.internal;

import java.util.ArrayList;
import java.util.List;

import fi.benjami.code4jvm.Value;

public class Scope {

	private final List<Value> stack;
	
	public Scope() {
		this.stack = new ArrayList<>();
	}
	
	public Scope(Scope other) {
		this.stack = new ArrayList<>(other.stack);
	}
	
	public void checkInputs(Value[] inputs, boolean allowStackPlacement) {
		if (inputs.length == 0) {
			return;
		}
		
		// Find if stack at least one of the requested inputs
		// In case there are multiple occurrences, search for the last one
		var matchStart = stack.lastIndexOf(inputs[0]);
		System.out.println(matchStart);
		
		// Count how many matches there are on stack
		// If stack usage is not allowed, ignore whatever is on stack
		// and create local variables instead
		var matchCount = 0;
		if (allowStackPlacement) {			
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
	
	public void addOutput(Value output) {
		stack.add(output);
	}
	
	public void resetStack() {
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
