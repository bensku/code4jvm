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
	private final LocalVar output;
	
	public CodeNode(Bytecode statement, LocalVar output) {
		this.statement = statement;
		this.output = output;
	}
	
	public void emitBytecode(MethodCompilerState state, Block block) {
		statement.emitBytecode(state, block);
		if (output.used) {
			storeOutput(state);
		} else {
			discardOutput(state.ctx());
		}
	}
	
	private void storeOutput(MethodCompilerState state) {
		if (output.needsSlot) {
			assert output.assignedSlot != -1 : "tried to store output to untracked LocalVar";
			state.ctx().asm().visitVarInsn(output.type().getOpcode(ISTORE, state.ctx()), output.assignedSlot);
		} // else: used but can be left on stack, do nothing
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
		if (output != null && output.needsSlot) {
			allocator.assignSlot(output); // Make sure slot is available
			frame.add(output);
		}
	}
	
	public Type outputType() {
		return statement.outputType();
	}
	
	public boolean outputToStack() {
		return output != null && output.used && !output.needsSlot;
	}
	
	@Override
	public String toString(DebugNames.Counting debugNameGen) {
		if (output != null && output.used) {
			return output.toString(debugNameGen) + " = " + statement;
		} else {
			return statement.toString();
		}
	}
}
