package fi.benjami.code4jvm.internal;

import java.util.ArrayList;
import java.util.Arrays;
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
		
		// Mark all inputs used
		var originals = Arrays.stream(inputs)
				.map(Value::original)
				.toArray(Value[]::new);
		for (var input : originals) {
			if (input instanceof LocalVar localVar) {
				localVar.used = true;
			}
		}
		
		// Compare inputs against the top values of stack
		// On failure, try with fewer inputs each time
		var inputsOnStack = 0;
		outer: for (var i = Math.max(0, stack.size() - inputs.length); i < stack.size(); i++) {
			var end = stack.size() - i;
			for (var j = 0; j < end; j++) {
				if (stack.get(i + j) != inputs[j]) {
					continue outer;
				}				
			}
			// Stack has at least some of the inputs
			inputsOnStack = end;
			break;
		}
		
		// Pop inputs that are on stack
		// This needs to be done to keep stack valid even if stack placement is disabled!
		for (var i = 0; i < inputsOnStack; i++) {
			stack.remove(stack.size() - 1);
		}
		
		// Mark local variable slot as required for rest of the inputs
		// If stack placement is disabled, do it for ALL inputs instead
		for (var i = allowStackPlacement ? inputsOnStack : 0; i < originals.length; i++) {
			if (originals[i] instanceof LocalVar localVar) {
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
