package fi.benjami.code4jvm.statement;

import static org.objectweb.asm.Opcodes.*;

import java.util.List;
import java.util.function.Consumer;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import fi.benjami.code4jvm.Block;
import fi.benjami.code4jvm.Constant;
import fi.benjami.code4jvm.Expression;
import fi.benjami.code4jvm.Value;
import fi.benjami.code4jvm.internal.LocalVar;
import fi.benjami.code4jvm.internal.SlotAllocator;
import fi.benjami.code4jvm.internal.CastValue;
import fi.benjami.code4jvm.internal.CompileContext;

public class Bytecode implements Expression {

	public static Bytecode run(Type outputType, List<Value> inputs, Consumer<MethodVisitor> emitter) {
		return new Bytecode(outputType, inputs, emitter);
	}
	
	public static Bytecode stub(Type outputType, List<Value> inputs) {
		return new Bytecode(outputType, inputs, null);
	}
	
	private final Type outputType;
	private final List<Value> inputs;
	private final Consumer<MethodVisitor> emitter;
	
	private Bytecode(Type outputType, List<Value> inputs, Consumer<MethodVisitor> emitter) {
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
	
	public List<Value> inputs() {
		return inputs;
	}
	
	
	public void emitBytecode(CompileContext ctx) {
		// Load inputs that are not in stack:
		// - Inputs that are not on stack directly before this statement
		// - Inputs that are in right place on stack, but need to be stored as local variables
		var mv = ctx.asm();
		var slotAllocator = ctx.slotAllocator();
		for (var input : inputs) {
			emitInput(mv, slotAllocator, input);
		}
		if (emitter != null) {			
			emitter.accept(ctx.asm()); // Emit user bytecode
		}
	}
	
	private void emitInput(MethodVisitor mv, SlotAllocator slotAllocator, Value input) {
		if (input instanceof Constant constant) {
			mv.visitLdcInsn(constant.value());
		} else if (input instanceof LocalVar localVar && localVar != LocalVar.EMPTY_MARKER) {
			if (!localVar.initialized) {
				throw new IllegalStateException("uninitialized value: " + localVar);
			}
			if (localVar.needsSlot) {
				mv.visitVarInsn(localVar.type().getOpcode(ILOAD), slotAllocator.get(localVar));
			} // else: already on stack
		} else if (input instanceof CastValue cast) {
			// Recursively emit the original, then the required cast
			emitInput(mv, slotAllocator, cast.original());
			cast.emitCast(mv);
		} else {
			throw new AssertionError("unknown input: " + input);
		}
	}
	
	public void discardOutput(CompileContext ctx) {
		if (outputType == Type.VOID_TYPE) {
			// No need to pop anything
		} else if (outputType == Type.LONG_TYPE || outputType == Type.DOUBLE_TYPE) {
			ctx.asm().visitInsn(POP2);
		} else {			
			ctx.asm().visitInsn(POP);
		}
	}
	
	public void storeOutput(CompileContext ctx, LocalVar localVar) {
		if (localVar.needsSlot) {
			var slot = ctx.slotAllocator().get(localVar);
			// Special handling for uninitialized values that should NOT be stored
			if (inputs.size() == 1 && inputs.get(0) == LocalVar.EMPTY_MARKER) {
				return;
			}
			ctx.asm().visitVarInsn(localVar.type().getOpcode(ISTORE), slot);
		}
		localVar.initialized = true;
	}
}
