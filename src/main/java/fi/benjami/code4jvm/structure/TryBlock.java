package fi.benjami.code4jvm.structure;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.objectweb.asm.Label;

import fi.benjami.code4jvm.Statement;
import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.Value;
import fi.benjami.code4jvm.Variable;
import fi.benjami.code4jvm.block.Block;
import fi.benjami.code4jvm.block.ReturnRedirect;
import fi.benjami.code4jvm.statement.Bytecode;
import fi.benjami.code4jvm.statement.Jump;
import fi.benjami.code4jvm.statement.Return;
import fi.benjami.code4jvm.statement.Throw;

/**
 * An equivalent to Java's try-catch-finally block.
 * 
 * <p>The main block is wrapped by
 * {@link #addCatch(Type, BiConsumer) catch blocks} for exceptions.
 * Additionally, <b>both</b> main and the catch blocks can be optionally wrapped by
 * {@link #addExitHook(Consumer) exit}, {@link #addReturnHook(BiConsumer)} and
 * {@link #addThrowHook(BiConsumer) throw} hooks.
 *
 */
public class TryBlock implements Statement {
	
	private record Catch(
			/**
			 * Type of exception to catch.
			 */
			Type type,
			Block handler,
			Variable caughtValue
	) {}
	
	/**
	 * Wraps the given block in a try block. It becomes the main block of it.
	 * @param main The main block.
	 * @return A try block.
	 */
	public static TryBlock of(Block main) {
		return new TryBlock(main);
	}
	
	/**
	 * Creates a new try block.
	 * @param callback Callback that receives the main block.
	 * @return A try block.
	 */
	public static TryBlock create(Consumer<Block> callback) {
		var main = Block.create("try");
		callback.accept(main);
		return new TryBlock(main);
	}
	
	private final Block main;
	
	private final List<Catch> catchBlocks;
	
	private Block exitHook;
	
	private Block returnHook;
	private final Variable returnedValue;
	
	private Block throwHook;
	private final Variable thrownValue;
	
	private TryBlock(Block main) {
		this.main = main;
		this.catchBlocks = new ArrayList<>();
		this.returnedValue = Variable.create(Type.METHOD_RETURN_TYPE);
		this.thrownValue = Variable.create(Type.of(Throwable.class));
	}
	
	/**
	 * Adds an exception handler block.
	 * @param exception Exception type to catch.
	 * @param handler Handler block.
	 * @return The caught exception, available inside the handler.
	 */
	public Value addCatch(Type exception, Block handler) {
		var parent = Block.create("catch " + exception); // Wrap handler to make caughtValue available
		var caughtValue = Variable.create(exception);
		parent.add(caughtValue.set(Value.stackTop(exception))); // Added to stack by VM
		parent.add(handler);
		catchBlocks.add(new Catch(exception, parent, caughtValue));
		return caughtValue;
	}
	
	/**
	 * Adds an exception handler block.
	 * @param exception Exception type to catch.
	 * @param callback Callback that receives the handler block and the caught
	 * exception as value.
	 * @return The caught exception, available inside the handler.
	 * @return This for chaining.
	 */
	public TryBlock addCatch(Type exception, BiConsumer<Block, Value> callback) {
		var handler = Block.create();
		var caughtValue = Variable.create(exception);
		handler.add(caughtValue.set(Value.stackTop(exception))); // Added to stack by VM
		
		// Don't call addCatch(Type, Block) to avoid unnecessary extra block
		catchBlocks.add(new Catch(exception, handler, caughtValue));
		callback.accept(handler, caughtValue);
		return this;
	}
	
	/**
	 * Adds an exit hook to this try block. Exit hooks are very similar to
	 * Java's finally blocks.
	 * 
	 * <p>Exit hooks are called after the main block and (if an exception was
	 * thrown and caught) catch blocks have finished execution. If the exit
	 * occurs by returning or throwing, the exit hook is executed but does not
	 * prevent the method from returning or throwing.
	 * @param hook Exit hook.
	 */
	public void addExitHook(Block hook) {
		exitHook = hook;
	}
	
	/**
	 * Adds an exit hook to this try block. Exit hooks are very similar to
	 * Java's finally blocks.
	 * 
	 * <p>Exit hooks are called after the main block and (if an exception was
	 * thrown and caught) catch blocks have finished execution. If the exit
	 * occurs by returning or throwing, the exit hook is executed but does not
	 * prevent the method from returning or throwing.
	 * @param hook Callback to receive the exit hook.
	 * @return This for chaining.
	 */
	public TryBlock addExitHook(Consumer<Block> callback) {
		var hook = Block.create("exit hook");
		callback.accept(hook);
		addExitHook(hook);
		return this;
	}
	
	/**
	 * Adds a return hook to this try block.
	 * 
	 * <p>Return hooks are executed whenever the main block or one of the catch
	 * blocks attempts to return from the method. The value that would have
	 * been returned is captured and the method is prevented from returning.
	 * @param hook Return hook.
	 * @return The captured return value.
	 */
	public Value addReturnHook(Block hook) {
		returnHook = hook;
		return returnedValue;
	}
	
	/**
	 * Adds a return hook to this try block.
	 * 
	 * <p>Return hooks are executed whenever the main block or one of the catch
	 * blocks attempts to return from the method. The value that would have
	 * been returned is captured and the method is prevented from returning.
	 * @param callback Callback that receives the return hook and captured
	 * return value.
	 * @return This for chaining.
	 */
	public TryBlock addReturnHook(BiConsumer<Block, Value> callback) {
		var hook = Block.create("return hook");
		var capturedValue = addReturnHook(hook);
		callback.accept(hook, capturedValue);
		return this;
	}
	
	/**
	 * Adds a throw hook to this try block.
	 * 
	 * <p>Throw hooks catch ALL exceptions that are thrown from main or by
	 * the catch blocks. Semantically, they are similar to wrapping a try
	 * block within another try block.
	 * @param hook Throw hook.
	 * @return The caught exception.
	 */
	public Value addThrowHook(Block hook) {
		throwHook = hook;
		return thrownValue;
	}
	
	/**
	 * Adds a throw hook to this try block.
	 * 
	 * <p>Throw hooks catch ALL exceptions that are thrown from main or by
	 * the catch blocks. Semantically, they are similar to wrapping a try
	 * block within another try block.
	 * @param callback Callback that receives the throw hook and caught
	 * exception.
	 * @return This for chaining.
	 */
	public TryBlock addThrowHook(BiConsumer<Block, Value> callback) {
		var hook = Block.create("throw hook");
		var capturedValue = addThrowHook(hook);
		callback.accept(hook, capturedValue);
		return this;
	}
	
	@Override
	public void emitVoid(Block block) {
		var root = Block.create("try block root");
		var outer = Block.create("try-catch");
		
		// Generate code for exit, return and throw hooks (Java 'finally' block with bells and whistles)
		// It is assumed that the exit hook is quite short, so we'll just copy it like javac does
		// In any case, jumping around would be difficult without jsr/ret
		
		var hasExitHandler = exitHook != null;
		var hasReturnHandler = hasExitHandler || returnHook != null;
		var hasThrowHandler = hasExitHandler || throwHook != null;
		
		var normalExit = Block.create("normal exit");
		if (exitHook != null) {
			normalExit.add(exitHook);
		}
		if (hasReturnHandler || hasThrowHandler) {
			// Jump over return and throw hooks after normal exit
			normalExit.add(Jump.to(root, Jump.Target.END));
		}
		Block exitViaReturn = null;
		ReturnRedirect returnRedirect = null;
		if (hasReturnHandler) {
			// Exit via return in main OR any of the catch blocks
			exitViaReturn = Block.create("return interceptor");

			// Capture return to a previously created local variable
			returnRedirect = new ReturnRedirect(exitViaReturn, returnedValue);
			
			if (exitHook != null) {
				exitViaReturn.add(exitHook.copy());
			}
			if (returnHook != null) {
				exitViaReturn.add(returnHook);
				// We don't know if the hook will actually return
				if (hasThrowHandler) {
					// Jump over throw handler just in case it doesn't
					exitViaReturn.add(Jump.to(root, Jump.Target.END));
				}
			} else {
				exitViaReturn.add(Return.value(returnedValue));
			}
		}
		Block exitViaThrow = null;
		if (hasThrowHandler) {			
			// Exit via throw in main OR any of the catch blocks
			exitViaThrow = Block.create("throw interceptor");
			// VM adds thrown value at top of the stack
			exitViaThrow.add(thrownValue.set(Value.stackTop(Type.of(Throwable.class))));
			if (exitHook != null) {
				exitViaThrow.add(exitHook.copy());
			}
			if (throwHook != null) {
				exitViaThrow.add(throwHook);
			} else {
				exitViaThrow.add(Throw.value(thrownValue)); // Re-throw by default
			}
			
			// Add catch-all handler to exception table
			var outerStart = outer.requestLabel(Jump.Target.START);
			var outerEnd = outer.requestLabel(Jump.Target.END);
			var handler = outer.add(new Block.Edge(exitViaThrow, Jump.Target.START, true, new Type[] {Type.of(Throwable.class)}));
			root.add(Bytecode.run(Type.VOID, new Value[0], ctx -> {
				ctx.asm().visitTryCatchBlock(outerStart, outerEnd, handler, null);
			}, "register catch-all handler"));
		}
		
		// Add edges for exception handlers
		Label[] labels = new Label[catchBlocks.size()];
		for (int i = 0; i < catchBlocks.size(); i++) {
			var clause = catchBlocks.get(i);
			labels[i] = outer.add(new Block.Edge(clause.handler, Jump.Target.START, true, new Type[] {clause.type}));
		}
		
		// Add main content to outer (not root) block
		outer.add(main);
		
		// Jump over catch blocks if no exception was thrown
		if (!catchBlocks.isEmpty()) {
			outer.add(Jump.to(outer, Jump.Target.END));
		}
		
		// Add catch blocks for exceptions to outer block
		if (!catchBlocks.isEmpty()) {
			// Main labels are exclusively for defining catch block boundaries
			var mainStart = main.requestLabel(Jump.Target.START);
			var mainEnd = main.requestLabel(Jump.Target.END);
			
			// Register exception handlers
			root.add(Bytecode.run(Type.VOID, new Value[0], ctx -> {
				for (int i = 0; i < catchBlocks.size(); i++) {
					ctx.asm().visitTryCatchBlock(mainStart, mainEnd, labels[i],
							catchBlocks.get(i).type().internalName());
				}
			}, "register exception handlers"));
			
			// Add catch blocks
			for (var clause : catchBlocks) {
				outer.add(clause.handler);
				// Handled exception is considered a normal exit
				outer.add(Jump.to(normalExit, Jump.Target.START));
			}
		}
		
		// Add outer to root and set up return capture if needed
		// This is needed since the exit hooks should be called even for
		// exception handlers
		if (hasReturnHandler) {
			outer.setReturnRedirect(returnRedirect);
		}
		root.add(outer);
		
		// Emit previously generated hooks after outer block, directly to root
		root.add(normalExit);
		if (exitViaReturn != null) {
			root.add(exitViaReturn);
		}
		if (exitViaThrow != null) {
			root.add(exitViaThrow);
		}
		
		block.add(root);
	}

}
