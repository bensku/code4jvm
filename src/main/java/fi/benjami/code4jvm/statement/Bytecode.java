package fi.benjami.code4jvm.statement;

import static org.objectweb.asm.Opcodes.*;

import java.util.Arrays;
import java.util.function.Consumer;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import fi.benjami.code4jvm.Expression;
import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.Value;
import fi.benjami.code4jvm.block.Block;
import fi.benjami.code4jvm.block.CompileContext;
import fi.benjami.code4jvm.internal.LocalVar;
import fi.benjami.code4jvm.internal.MethodCompilerState;
import fi.benjami.code4jvm.internal.SharedSecrets;
import fi.benjami.code4jvm.internal.ValueTools;

public class Bytecode implements Expression {
	
	public interface Emitter {
		void emit(MethodVisitor mv);
		
		default void emit(MethodVisitor mv, CompileContext ctx) {
			emit(mv);
		}
	}
	
	public static Bytecode run(Type outputType, Value[] inputs, Consumer<CompileContext> emitter) {
		return new Bytecode(outputType, inputs, emitter);
	}
	
	public static Bytecode stub(Type outputType, Value[] inputs) {
		return new Bytecode(outputType, inputs, null);
	}
	
	public static Label requestLabel(Block block, Jump.Target position) {
		return SharedSecrets.LABEL_GETTER.apply(block, position);
	}
	
	private final Type outputType;
	private final Value[] inputs;
	private final Consumer<CompileContext> emitter;
	
	private Bytecode(Type outputType, Value[] inputs, Consumer<CompileContext> emitter) {
		this.outputType = outputType;
		this.inputs = inputs;
		this.emitter = emitter;
	}

	@Override
	public Value emitValue(Block block) {
		throw new UnsupportedOperationException("bytecode is emitted later");
	}
	
	public Type outputType() {
		return outputType;
	}
	
	public Value[] inputs() {
		return inputs;
	}
	
	
	public void emitBytecode(MethodCompilerState state) {
		// Load inputs that are not in stack:
		// - Inputs that are not on stack directly before this statement
		// - Inputs that are in right place on stack, but need to be stored as local variables
		var ctx = state.ctx();
		for (var input : inputs) {
			ValueTools.emitInput(state, input);
		}
		if (emitter != null) {			
			emitter.accept(ctx); // Emit user bytecode
		}
	}
	
	public void discardOutput(CompileContext ctx) {
		if (outputType == Type.VOID) {
			// No need to pop anything
		} else if (outputType == Type.LONG || outputType == Type.DOUBLE) {
			ctx.asm().visitInsn(POP2);
		} else {			
			ctx.asm().visitInsn(POP);
		}
	}
	
	public void storeOutput(MethodCompilerState state, LocalVar localVar) {
		// Special handling for uninitialized values that should NOT be stored
		// Currently, they are only created by LocalVar#uninitialized(Type)
		var initialize = inputs.length != 1 || inputs[0] != LocalVar.EMPTY_MARKER;
		if (localVar.needsSlot) {
			var slot = state.slotAllocator().get(localVar);
			if (initialize) {
				state.ctx().asm().visitVarInsn(localVar.type().getOpcode(ISTORE, state.ctx()), slot);
			}
		}
		if (initialize) {
			localVar.initialized = true;
		}
	}
}
