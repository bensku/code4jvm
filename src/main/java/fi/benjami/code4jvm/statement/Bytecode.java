package fi.benjami.code4jvm.statement;

import java.util.Arrays;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import fi.benjami.code4jvm.Expression;
import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.Value;
import fi.benjami.code4jvm.block.Block;
import fi.benjami.code4jvm.block.CompileContext;
import fi.benjami.code4jvm.internal.DebugNames;
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
	
	public static Bytecode run(Type outputType, Value[] inputs, BiConsumer<CompileContext, Block> emitter, int flags, Object debugName) {
		return new Bytecode(outputType, inputs, emitter, 0, debugName);
	}
	
	public static Bytecode run(Type outputType, Value[] inputs, BiConsumer<CompileContext, Block> emitter, Object debugName) {
		return run(outputType, inputs, emitter, 0, debugName);
	}
	
	public static Bytecode run(Type outputType, Value[] inputs, Consumer<CompileContext> emitter, int flags, Object debugName) {
		return new Bytecode(outputType, inputs, (ctx, block) -> emitter.accept(ctx), flags, debugName);
	}
	
	public static Bytecode run(Type outputType, Value[] inputs, Consumer<CompileContext> emitter, Object debugName) {
		return run(outputType, inputs, emitter, 0, debugName);
	}
	
	public static Bytecode stub(Type outputType, Value[] inputs) {
		// Flags don't currently make any sense for stub,
		// because the only thing the stub does is load the inputs to stack
		return new Bytecode(outputType, inputs, null, 0, "stub");
	}
	
	public static String name(String name) {
		return name;
	}
	
	public static Supplier<String> name(String fmt, Object... args) {
		return () -> String.format(fmt, args);
	}
	
	private final Type outputType;
	private final Value[] inputs;
	private final BiConsumer<CompileContext, Block> emitter;
	private final int flags;
	private final Object debugName;
	
	private Bytecode(Type outputType, Value[] inputs, BiConsumer<CompileContext, Block> emitter, int flags, Object debugName) {
		this.outputType = outputType;
		this.inputs = inputs;
		this.emitter = emitter;
		this.flags = flags;
		this.debugName = debugName;
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
		// Load inputs that are not in stack (and keep track of stack size)
		// - Inputs that are not on stack directly before this statement
		// - Inputs that are in right place on stack, but need to be stored as local variables
		var ctx = state.ctx();
		ctx.stack().reserveStack(inputs);
		if ((flags & EXPLICIT_LOAD) == 0) {
			// If explicit loads are disabled, we'll automatically load inputs
			// It just happens that we can use the same API as user code
			ctx.stack().loadExplicit(inputs);
		}
		if (emitter != null) {
			emitter.accept(ctx, block); // Emit user bytecode
		}
	}
	
	@Override
	public String toString() {
		return toString(new DebugNames.Counting("var_"));
	}
	
	public String toString(DebugNames.Counting argsNameGen) {
		var args = Arrays.stream(inputs)
				.map(arg -> arg instanceof LocalVar localVar
						? localVar.toString(argsNameGen) : arg.toString())
				.collect(Collectors.joining(", "));
		// If name is supplier, call it; otherwise, just call toString
		var name = debugName instanceof Supplier<?> supplier ? supplier.get() : debugName;
		return name + " (" + args + "): " + outputType;
	}

}
