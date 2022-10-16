package fi.benjami.code4jvm.block;

import java.util.stream.Collectors;

import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.internal.DebugNames;
import fi.benjami.code4jvm.internal.SlotAllocator;

public sealed class ConcreteMethod extends Routine implements Method
		permits Method.Static, Method.Instance {
	
	private final String name;
	
	/**
	 * Bytecode access field, EXCLUDING actual {@link Access access}
	 * (visibility) of method, that is determined only when it is added to a
	 * class.
	 */
	final int access;
	
	boolean framesComputed;
	
	/**
	 * Slot allocator from first compilation. Frames are built once only, but
	 * compiling this again needs {@link SlotAllocator#findVar(int)}.
	 */
	SlotAllocator slotAllocator;
	
	ConcreteMethod(Block block, Type returnType, String name, int access) {
		super(block, returnType);
		this.name = name;
		this.access = access;
	}
	
	public String name() {
		return name;
	}
	
	@Override
	public String toString() {
		// Need to create debug name generator for local variables here,
		// because method arguments are local variables like any others
		var localNameGen = new DebugNames.Counting("var_");
		var argsStr = args.stream()
				.map(arg -> arg.toString(localNameGen))
				.collect(Collectors.joining(", "));
		return "method " + name
				+ "(" + argsStr + ")"
				+ ": " + returnType() + " {\n"
				+ block().toString(localNameGen, framesComputed, "    ")
				+ "}";
	}
}
