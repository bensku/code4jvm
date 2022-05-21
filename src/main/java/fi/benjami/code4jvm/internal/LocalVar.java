package fi.benjami.code4jvm.internal;

import java.util.Optional;

import fi.benjami.code4jvm.Statement;
import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.Value;
import fi.benjami.code4jvm.Variable;
import fi.benjami.code4jvm.block.Block;
import fi.benjami.code4jvm.statement.Bytecode;
import fi.benjami.code4jvm.util.TypeCheck;

import static org.objectweb.asm.Opcodes.*;

/**
 * A value that is can be stored on a local variable slot if needed.
 *
 */
public class LocalVar implements Variable {
	
	/**
	 * When used as the only input for {@link Bytecode}, the returned value
	 * is never stored. Used for
	 * {@link Value#uninitialized(Type) uninitialized values}.
	 */
	public static final LocalVar EMPTY_MARKER = new LocalVar(null, null);

	private final Type type;
	private final Block parentBlock;
	private String name;

	/**
	 * If this value has been initialized. Most expressions (except
	 * {@link Value#uninitialized(Type)}) return values that have been
	 * initialized. {@link #set(Value) Setting} a variable also makes it
	 * initialized. Uninitialized values cannot be used as inputs for
	 * {@link Bytecode}.
	 * 
	 * <p>Used in bytecode generation phase.
	 */
	public boolean initialized;
	
	/**
	 * Whether or not this needs a local variable slot. Values that are used
	 * immediately after they've been added to stack don't always need slots.
	 * 
	 * @see Scope
	 * @see Bytecode
	 */
	public boolean needsSlot;
	
	/**
	 * Slot this variable is assigned to. Use {@link SlotAllocator#get(LocalVar)}
	 * instead of accessing this directly.
	 */
	int assignedSlot;
	
	public LocalVar(Type type, Block parentBlock) {
		this.type = type;
		this.parentBlock = parentBlock;
		this.name = null;
		this.needsSlot = false;
		this.assignedSlot = -1;
	}

	@Override
	public Type type() {
		return type;
	}

	@Override
	public Optional<Block> parentBlock() {
		return Optional.of(parentBlock);
	}

	@Override
	public Optional<String> name() {
		return Optional.ofNullable(name);
	}
	
	public void name(String name) {
		this.name = name;
	}

	@Override
	public Statement set(Value value) {
		TypeCheck.mustEqual(this, value);
		return block -> {
			block.add(Bytecode.run(Type.VOID, new Value[] {value}, mv -> {
				if (needsSlot) {
					assert assignedSlot != -1 : "set(Value) on untracked variable " + toString();
					mv.visitVarInsn(type.getOpcode(ISTORE), assignedSlot);
				} // else: nothing seems to read this variable, so stores to it don't matter
				initialized = true; // This variable received a value
			}));
		};
	}
	
	@Override
	public String toString() {
		return "LocalVar{" + type + " " + assignedSlot + (name != null ? " " + name : "") + "}";
	}
}
