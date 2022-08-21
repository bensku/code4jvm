package fi.benjami.code4jvm.internal.node;

import fi.benjami.code4jvm.UninitializedValueException;
import fi.benjami.code4jvm.block.Block;
import fi.benjami.code4jvm.internal.Frame;
import fi.benjami.code4jvm.internal.LocalVar;
import fi.benjami.code4jvm.internal.MethodCompilerState;
import fi.benjami.code4jvm.internal.SlotAllocator;
import fi.benjami.code4jvm.statement.Bytecode;

public final class CodeNode implements Node {
	
	private final Bytecode statement;
	private LocalVar assignedVar;
	
	public CodeNode(Bytecode statement) {
		this.statement = statement;
	}
	
	public void assignVar(LocalVar localVar) {
		this.assignedVar = localVar;
	}
	
	public void emitBytecode(MethodCompilerState state, Block block) {
		statement.emitBytecode(state, block);
		if (assignedVar != null && assignedVar.used) {
			statement.storeOutput(state, assignedVar);
		} else {
			statement.discardOutput(state.ctx());
		}
	}
	
	public void validateInputs(SlotAllocator allocator, Frame frame, Block parent) {
		for (var input : statement.inputs()) {
			// Ignore constants and on-stack variables
			// Scope makes only variables that are not on stack use slots
			if (input.original() instanceof LocalVar localVar && localVar.needsSlot) {
				allocator.assignSlot(localVar); // Make sure slot is available
				if (!frame.has(localVar)) {
					throw new UninitializedValueException(localVar, parent);
				}
			}
		}
	}
	
	public void addOutput(SlotAllocator allocator, Frame frame) {
		if (assignedVar != null && assignedVar.needsSlot) {
			allocator.assignSlot(assignedVar); // Make sure slot is available
			frame.add(assignedVar);
		}
	}
}
