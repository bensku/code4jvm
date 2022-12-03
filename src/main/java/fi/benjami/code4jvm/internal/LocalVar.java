package fi.benjami.code4jvm.internal;

import java.util.Optional;

import fi.benjami.code4jvm.Statement;
import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.Value;
import fi.benjami.code4jvm.Variable;
import fi.benjami.code4jvm.internal.node.StoreNode;
import fi.benjami.code4jvm.statement.Bytecode;
import fi.benjami.code4jvm.util.TypeCheck;

/**
 * A value that is can be stored on a local variable slot if needed.
 *
 */
public class LocalVar implements Variable {
	
	private final Type type;
	private String name;
	
	/**
	 * Whether or not this value is used as an input. Unused values are
	 * discarded.
	 */
	public boolean used;
	
	/**
	 * Whether or not this needs a local variable slot. Values that are used
	 * immediately after they've been added to stack don't always need slots.
	 * 
	 * @see Scope
	 * @see Bytecode
	 */
	public boolean needsSlot;
	
	/**
	 * Slot this variable is assigned to. This is assigned by a
	 * {@link slot allocator} when stack map frames are built.
	 */
	public int assignedSlot;
	
	public LocalVar(Type type) {
		this.type = type;
		this.name = null;
		this.needsSlot = false;
		this.assignedSlot = -1;
	}

	@Override
	public Type type() {
		return type;
	}

	@Override
	public Optional<String> name() {
		return Optional.ofNullable(name);
	}
	
	public void name(String name) {
		this.name = name;
	}
	
	@Override
	public Value original() {
		return this;
	}

	@Override
	public Statement set(Value value) {
		TypeCheck.mustEqual(this, value);
		return new StoreNode(this, value);
	}
	
	@Override
	public String toString() {
		return "var{" + type + " "
				+ name().orElse("UNKNOWN")
				+ ", " + (needsSlot ? "slot " + assignedSlot : "stack")
				+ "}";
	}
	
	public String toString(DebugNames.Counting debugNameGen) {
		return "var{" + type + " "
				+ name().orElse(debugNameGen.make(this))
				+ ", " + (needsSlot ? "slot " + assignedSlot : "stack")
				+ "}";
	}
}
