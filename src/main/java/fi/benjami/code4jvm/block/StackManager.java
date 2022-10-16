package fi.benjami.code4jvm.block;

import static org.objectweb.asm.Opcodes.*;

import org.objectweb.asm.MethodVisitor;

import fi.benjami.code4jvm.Constant;
import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.Value;
import fi.benjami.code4jvm.internal.CastValue;
import fi.benjami.code4jvm.internal.LocalVar;
import fi.benjami.code4jvm.internal.StackTop;
import fi.benjami.code4jvm.statement.Bytecode;
import fi.benjami.code4jvm.util.TypeUtils;

/**
 * Low-level tools for reserving stack space and loading values there. Most
 * {@link Bytecode bytecode} statements don't need to use these at all.
 *
 */
public class StackManager {
	
	/**
	 * Parent compilation context.
	 */
	private final CompileContext ctx;
	
	/**
	 * Current stack size.
	 */
	private int currentStack;
	
	/**
	 * Maximum stack size of the entire method.
	 */
	private int maxStack;

	/**
	 * Stack slots that the current node reserves. This is in addition to what
	 * is already on stack!
	 */
	private int reservedStack;
	
	/**
	 * Stack slots consumed by the current node. If the node uses inputs that
	 * are already on stack and produces no output to stack, this could be 
	 * larger than the reserved stack.
	 */
	private int consumedStack;
	
	StackManager(CompileContext ctx) {
		this.ctx = ctx;
	}
	
	/**
	 * Makes sure that there is enough stack space for the given values.
	 * Space for bytecode {@link Bytecode#inputs() inputs} and
	 * {@link Bytecode#outputType() output} is reserved automatically, so
	 * calling this from user code is usually unnecessary.
	 * @param values Values that might need to be on stack.
	 * @implNote Internally, this is called by emitBytecode() methods from
	 * various places.
	 */
	public void reserveStack(Value... values) {
		for (Value value : values) {
			reserveStack(value);
		}
	}
	
	/**
	 * Makes sure that there is enough stack space for the given value.
	 * Space for bytecode {@link Bytecode#inputs() inputs} and
	 * {@link Bytecode#outputType() output} is reserved automatically, so
	 * calling this from user code is usually unnecessary.
	 * @param value Value that might need to be on stack.
	 */
	public void reserveStack(Value value) {
		int newSlots = TypeUtils.slotCount(value.type());
		var original = value.original();
		var originalSlots = value != original ? TypeUtils.slotCount(original.type()) : newSlots;
		if (original instanceof LocalVar localVar && !localVar.needsSlot) {
			// LocalVar already on stack, but if it is cast to long/double,
			// additional stack slot might be needed
			reservedStack += Math.max(0, newSlots - originalSlots);
		} else {
			// For all other values, including constants, stack size is increased
			reservedStack += Math.max(originalSlots, newSlots);
		}
		// In ALL cases, stack used/reserved by node should be consumed during its execution
		consumedStack += Math.max(originalSlots, newSlots);
	}
	
	/**
	 * Reserves the given amount of stack slots. This could be needed if
	 * custom {@link Bytecode} needs to temporarily use space to
	 * e.g. duplicate values on stack. 
	 * @param slots Slot count.
	 */
	public void reserveStack(int slots) {
		reservedStack += slots;
		consumedStack += slots;
	}
	
	/**
	 * Releases the currently reserved stack.
	 * @param outputType Output type of node.
	 * @param outputToStack True if the output will stay on stack, false if it
	 * is either discarded or stored to a local variable.
	 */
	void releaseStack(Type outputType, boolean outputToStack) {
		var returnSlots = TypeUtils.slotCount(outputType);
		
		// Check if a new maximum stack size was reached
		// If reserved stack is not enough for output produced by the node,
		// use slots needed for it in the computation
		// Otherwise a non-void node that produces an output might run out
		// of stack space!
		currentStack += Math.max(reservedStack, returnSlots);
		if (currentStack > maxStack) {
			maxStack = currentStack;
		}
		
		// Consume values reserved/used by current node from stack
		assert currentStack - consumedStack >= 0;
		currentStack -= consumedStack;
		
		// Reset per-node counters
		reservedStack = 0;
		consumedStack = 0;
		
		// Increase current stack size if output stays on stack
		// (void output doesn't, and has zero returnSlots)
		if (outputToStack) {
			currentStack += returnSlots;
		}
	}
	
	/**
	 * Gets the maximum stack size of method. Do not call this until all
	 * bytecode has been emitted!
	 * @return Maximum stack size.
	 */
	int maxStackSize() {
		return maxStack;
	}
	
	/**
	 * Loads the given values to stack.
	 * @param inputs Values to load to stack.
	 * @see Bytecode#EXPLICIT_LOAD
	 */
	public void loadExplicit(Value... inputs) {
		for (var input : inputs) {
			loadExplicit(input);
		}
	}
	
	/**
	 * Loads the given value to stack.
	 * @param input Value to load to stack.
	 * @see Bytecode#EXPLICIT_LOAD
	 */
	public void loadExplicit(Value input) {
		if (input instanceof Constant constant) {
			emitConstant(constant);
		} else if (input instanceof LocalVar localVar) {
			if (localVar.needsSlot) {
				ctx.asm().visitVarInsn(localVar.type().getOpcode(ILOAD, ctx), localVar.assignedSlot);
			} // else: already on stack
		} else if (input instanceof CastValue cast) {
			// Recursively emit the original, then the required cast
			loadExplicit(cast.original());
			cast.emitCast(ctx.asm());
		} else if (input instanceof StackTop) {
			// Do nothing, value is already on stack
		} else {
			throw new AssertionError("unknown input: " + input);
		}
	}
	
	private void emitConstant(Constant constant) {
		MethodVisitor mv = ctx.asm();
		
		var type = constant.type();
		if (type.equals(Type.BOOLEAN)) {
			var value = (boolean) constant.value();
			// true == 1, false == 0
			mv.visitInsn(value ? ICONST_1 : ICONST_0);
		} else if (TypeUtils.isIntLike(type)) {
			int value;
			if (type.equals(Type.CHAR)) {
				// Although char is int for JVM, Character is not a Number in Java
				value = ((Character) constant.value()).charValue();
			} else {
				// Although byte and short are just ints for JVM, the boxed types
				// are not directly related and CANNOT be cast to each other
				value = ((Number) constant.value()).intValue();
			}
			switch (value) {
			case -1 -> mv.visitInsn(ICONST_M1);
			case 0 -> mv.visitInsn(ICONST_0);
			case 1 -> mv.visitInsn(ICONST_1);
			case 2 -> mv.visitInsn(ICONST_2);
			case 3 -> mv.visitInsn(ICONST_3);
			case 4 -> mv.visitInsn(ICONST_4);
			case 5 -> mv.visitInsn(ICONST_5);
			default -> {
				if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
					// Fits to byte
					mv.visitIntInsn(BIPUSH, value);
				} else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
					// Fits to short
					mv.visitIntInsn(SIPUSH, value);
				} else {
					// Need to add to constant table
					mv.visitLdcInsn(value);
				}
			}
			}
		} else if (type.equals(Type.FLOAT)) {
			var value = (float) constant.value();
			if (value == 0) {
				mv.visitInsn(FCONST_0);
			} else if (value == 1) {
				mv.visitInsn(FCONST_1);
			} else if (value == 2) {
				mv.visitInsn(FCONST_2);
			} else {
				mv.visitLdcInsn(value);
			}
		} else if (type.equals(Type.LONG)) {
			var value = (long) constant.value();
			if (value == 0) {
				mv.visitInsn(LCONST_0);
			} else if (value == 1) {
				mv.visitInsn(LCONST_1);
			} else {
				mv.visitLdcInsn(value);
			}
		} else if (type.equals(Type.DOUBLE)) {
			var value = (double) constant.value();
			if (value == 0) {
				mv.visitInsn(DCONST_0);
			} else if (value == 1) {
				mv.visitInsn(DCONST_1);
			} else {
				mv.visitLdcInsn(value);
			}
		} else {
			mv.visitLdcInsn(constant.asmValue());
		}
	}
}
