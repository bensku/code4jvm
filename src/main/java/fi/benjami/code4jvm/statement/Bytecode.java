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

public class Bytecode implements Expression {
	
	/**
	 * Setting this flag disables the automatic loading of
	 * {@link #inputs() inputs}. This allows user code to select which of the
	 * inputs should be loaded, while still using code4jvm value system.
	 * 
	 * <p>Warning: This disables consuming inputs from stack, because it is
	 * unknown to code4jvm which of the inputs are loaded, and in which order
	 * they are loaded. This may result in bigger class files and could have
	 * a small runtime performance impact.
	 * 
	 * @see CompileContext#loadExplicit(Value[])
	 * @see StringConcat
	 */
	public static final int EXPLICIT_LOAD = 1;
	
	public static Bytecode run(Type outputType, Value[] inputs, BiConsumer<CompileContext, Block> emitter, int flags) {
		return new Bytecode(outputType, inputs, emitter, 0);
	}
	
	public static Bytecode run(Type outputType, Value[] inputs, BiConsumer<CompileContext, Block> emitter) {
		return run(outputType, inputs, emitter, 0);
	}
	
	public static Bytecode run(Type outputType, Value[] inputs, Consumer<CompileContext> emitter, int flags) {
		return new Bytecode(outputType, inputs, (ctx, block) -> emitter.accept(ctx), flags);
	}
	
	public static Bytecode run(Type outputType, Value[] inputs, Consumer<CompileContext> emitter) {
		return run(outputType, inputs, emitter, 0);
	}
	
	public static Bytecode stub(Type outputType, Value[] inputs) {
		// Flags don't currently make any sense for stub,
		// because the only thing the stub does is load the inputs to stack
		return new Bytecode(outputType, inputs, null, 0);
	}
	
	private final Type outputType;
	private final Value[] inputs;
	private final BiConsumer<CompileContext, Block> emitter;
	private final int flags;
	
	private Bytecode(Type outputType, Value[] inputs, BiConsumer<CompileContext, Block> emitter, int flags) {
		this.outputType = outputType;
		this.inputs = inputs;
		this.emitter = emitter;
		this.flags = flags;
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
	
	public int flags() {
		return flags;
	}
	
	public void emitBytecode(MethodCompilerState state, Block block) {
		// Load inputs that are not in stack:
		// - Inputs that are not on stack directly before this statement
		// - Inputs that are in right place on stack, but need to be stored as local variables
		var ctx = state.ctx();
		if ((flags & EXPLICIT_LOAD) == 0) {
			// If explicit loads are disabled, we'll automatically load inputs
			// It just happens that we can use the same API as user code
			ctx.loadExplicit(inputs);
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
			if (initialize) {
				assert localVar.assignedSlot != -1 : "tried to store output to untracked LocalVar";
				state.ctx().asm().visitVarInsn(localVar.type().getOpcode(ISTORE, state.ctx()), localVar.assignedSlot);
			}
		}
	}
}
