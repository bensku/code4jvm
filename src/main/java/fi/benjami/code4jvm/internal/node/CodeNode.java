package fi.benjami.code4jvm.internal.node;

import static org.objectweb.asm.Opcodes.ISTORE;
import static org.objectweb.asm.Opcodes.POP;
import static org.objectweb.asm.Opcodes.POP2;

import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.UninitializedValueException;
import fi.benjami.code4jvm.block.Block;
import fi.benjami.code4jvm.block.CompileContext;
import fi.benjami.code4jvm.block.Method;
import fi.benjami.code4jvm.internal.Frame;
import fi.benjami.code4jvm.internal.LocalVar;
import fi.benjami.code4jvm.internal.MethodCompilerState;
import fi.benjami.code4jvm.internal.DebugNames;
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
			storeOutput(state, assignedVar);
		} else {
			discardOutput(state.ctx());
		}
	}
	
	public void storeOutput(MethodCompilerState state, LocalVar localVar) {
		if (localVar.needsSlot) {
			assert localVar.assignedSlot != -1 : "tried to store output to untracked LocalVar";
			state.ctx().asm().visitVarInsn(localVar.type().getOpcode(ISTORE, state.ctx()), localVar.assignedSlot);
		}
	}
	
	private void discardOutput(CompileContext ctx) {
		var outputType = statement.outputType();
		if (outputType == Type.VOID) {
			// No need to pop anything
		} else if (outputType == Type.LONG || outputType == Type.DOUBLE) {
			ctx.asm().visitInsn(POP2);
		} else {			
			ctx.asm().visitInsn(POP);
		}
	}
	
	public void validateInputs(SlotAllocator allocator, Frame frame, Block parent, Method method) {
		for (var input : statement.inputs()) {
			// Ignore constants and on-stack variables
			// Scope makes only variables that are not on stack use slots
			if (input.original() instanceof LocalVar localVar && localVar.needsSlot) {
				allocator.assignSlot(localVar); // Make sure slot is available
				if (!frame.has(localVar)) {
					throw new UninitializedValueException(localVar, parent, method);
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
	
	public Type outputType() {
		return statement.outputType();
	}
	
	public boolean outputToStack() {
		return assignedVar != null && assignedVar.used && !assignedVar.needsSlot;
	}
	
	@Override
	public String toString(DebugNames.Counting debugNameGen) {
		if (assignedVar != null && assignedVar.used) {
			return assignedVar.toString(debugNameGen) + " = " + statement;
		} else {
			return statement.toString();
		}
	}
}
