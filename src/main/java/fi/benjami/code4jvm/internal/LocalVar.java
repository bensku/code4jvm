package fi.benjami.code4jvm.internal;


import java.util.List;
import java.util.Optional;

import org.objectweb.asm.Type;

import fi.benjami.code4jvm.Block;
import fi.benjami.code4jvm.Statement;
import fi.benjami.code4jvm.Value;
import fi.benjami.code4jvm.Variable;
import fi.benjami.code4jvm.statement.Bytecode;
import fi.benjami.code4jvm.util.TypeCheck;

import static org.objectweb.asm.Opcodes.*;

public class LocalVar implements Variable {

	private final Type type;
	private final Block parentBlock;
	private String name;
	
	public boolean needsSlot;
	public int loadCount;
	
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
		this.loadCount = 0;
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
			block.add(Bytecode.run(Type.VOID_TYPE, List.of(value), mv -> {
				if (assignedSlot != -1) {					
					mv.visitVarInsn(type.getOpcode(ISTORE), assignedSlot);
				} // else: nothing seems to read this variable, so stores to it don't matter
			}));
		};
	}
	
	@Override
	public String toString() {
		return "LocalVar{" + type + " " + assignedSlot + "}";
	}
}
