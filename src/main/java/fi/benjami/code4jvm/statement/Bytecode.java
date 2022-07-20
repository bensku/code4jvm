package fi.benjami.code4jvm.statement;

import static org.objectweb.asm.Opcodes.*;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import fi.benjami.code4jvm.Expression;
import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.Value;
import fi.benjami.code4jvm.block.Block;
import fi.benjami.code4jvm.block.CompileContext;
import fi.benjami.code4jvm.internal.LocalVar;
import fi.benjami.code4jvm.internal.MethodCompilerState;
import fi.benjami.code4jvm.internal.ValueTools;

public class Bytecode implements Expression {
	
	public static Bytecode run(Type outputType, Value[] inputs, BiConsumer<CompileContext, Block> emitter) {
		return new Bytecode(outputType, inputs, emitter);
	}
	
	public static Bytecode run(Type outputType, Value[] inputs, Consumer<CompileContext> emitter) {
		return new Bytecode(outputType, inputs, (ctx, block) -> emitter.accept(ctx));
	}
	
	public static Bytecode stub(Type outputType, Value[] inputs) {
		return new Bytecode(outputType, inputs, null);
	}
	
	private final Type outputType;
	private final Value[] inputs;
	private final BiConsumer<CompileContext, Block> emitter;
	
	private Bytecode(Type outputType, Value[] inputs, BiConsumer<CompileContext, Block> emitter) {
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
	
	
	public void emitBytecode(MethodCompilerState state, Block block) {
		// Load inputs that are not in stack:
		// - Inputs that are not on stack directly before this statement
		// - Inputs that are in right place on stack, but need to be stored as local variables
		var ctx = state.ctx();
		for (var input : inputs) {
			ValueTools.emitInput(state, input);
		}
		if (emitter != null) {
			emitter.accept(ctx, block); // Emit user bytecode
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
	}
}
