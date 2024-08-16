package fi.benjami.code4jvm.lua.runtime;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Function;

import fi.benjami.code4jvm.lua.LuaVm;
import fi.benjami.code4jvm.lua.stdlib.LuaException;

/**
 * Lua coroutines are a form of collaborative multithreading. Only one can be
 * active at a time per single {@link LuaVm}, and the coroutines must
 * explicitly yield to to hand over control to others. However, creating
 * coroutines is very, very cheap.
 * 
 * <p>Lua4jvm's coroutines support execution of arbitrary Java code. It is
 * even possible to keep Java code running on background while the coroutine
 * appears (for Lua code) to be suspended. The only restriction that at most
 * one thread may ever execute Lua code or inspect Lua tables at a time.
 * 
 * @implNote Lua coroutines are backed by JVM virtual threads.
 */
public class LuaCoroutine {
	
	public record Result(
			boolean success,
			Object[] values
	) {}
	
	private static final ThreadLocal<LuaCoroutine> current = new ThreadLocal<>();
	
	/**
	 * Creates a new coroutine and leaves it {@link Status#SUSPENDED suspended},
	 * waiting to be {@link #resume(Object...) resumed}.
	 * @param main Main function of the coroutine. It receives the arguments
	 * from first call to {@link #resume(Object...)}. The values it returns are
	 * returned <i>by</i> last call to the same method.
	 * @return A new Lua coroutine.
	 * 
	 */
	public static LuaCoroutine create(Function<Object[], Object[]> main) {
		var coroHolder = new AtomicReference<LuaCoroutine>();
		var thread = Thread.ofVirtual().unstarted(() -> {
			// Setup coroutine, then wait for first call to resume
			var coro = coroHolder.getPlain(); // Thread start implies happens-before relationship, so this is safe
			current.set(coro);
			var args = waitForResume(coro);
			
			try {
				var results = main.apply(args);
				// After coroutine ends, return control and its return values to parent
				wakeUpParent(coro, true, results);
			} catch (Throwable e) {
				// If there is any exception or even error; catch it and return control to parent
				wakeUpParent(coro, false, e);
			} finally {
				waitForResume(coro); // Wait for parent to collect the results
				coro.status = Status.DEAD;
			}
		});
		var coro = new LuaCoroutine(thread);
		coroHolder.setPlain(coro);
		
		thread.start();
		
		return coro;
	}
	
	/**
	 * Yields from the currently executing Lua coroutine. This call blcoks the
	 * current thread until the coroutine is resumed again.
	 * @param values Values to yield. These are returned to whoever
	 * originally called {@link #resume(Object...)} on this coroutine.
	 * @return Values that resume wanted to pass to this coroutine.
	 */
	public static Object[] yield(Object... values) {
		var coro = current.get();
		if (coro == null) {
			throw new LuaException("attempt to yield from outside a coroutine");
		}
		
		coro.status = Status.SUSPENDED; // Make this look suspended to parent
		wakeUpParent(coro, true, values); // Wake up parent
		return waitForResume(coro); // Actually suspend this
	}
	
	public static class DetachTicket implements AutoCloseable {

		private DetachTicket() {}
		
		@Override
		public void close() {
			// We're exiting code detached from Lua VM
			// Now it is time to actually suspend this thread!
			var coro = current.get();
			coro.autoYield = false;
			coro.yieldResult = null;
			waitForResume(coro);
		}
		
	}
	
	/**
	 * Detaches the currently executing thread from its Lua coroutine.
	 * To whoever called {@link #resume(Object...)}, this appears as if the
	 * coroutine would continually yield the given values. However, we can
	 * continue executin of Java code.
	 * 
	 * <p><b>Warning:</b> Do not access, in any way, the Lua VM that considers
	 * the coroutine to be yielded. Doing so may cause the VM to crash (or worse).
	 * @param values Values to yield. These are returned every time resumption
	 * of the coroutine is attempted.
	 * @return Ticket for thread detachment. When it is closed, the current
	 * thread will sleep until resume is called once more. After this, it is
	 * safe to continue accessing the Lua VM.
	 */
	public static DetachTicket detachYield(Object... values) {
		var coro = current.get();
		if (coro == null) {
			throw new IllegalStateException("current thread is not Lua coroutine");
		}
		
		coro.autoYield = true;
		coro.status = Status.SUSPENDED; // Make this look suspended (to Lua code, anyway)
		wakeUpParent(coro, true, values); // Wake up parent
		return new DetachTicket(); // ... but return control to Java code
	}
	
	private static void wakeUpParent(LuaCoroutine coro, boolean success, Object... yieldValues) {
		assert coro.parent != null;
		coro.yieldResult = new Result(success, yieldValues);
		LockSupport.unpark(coro.parent);
		coro.parent = null;
	}
	
	private static Object[] waitForResume(LuaCoroutine coro) {
		// Wait for this to be resumed again in future; in loop to guard against spurious wakeups
		while (coro.status == Status.SUSPENDED) {			
			LockSupport.park();
		}
		
		// We've been resumed; return values passed from other side
		var results = coro.resumeValues;
		coro.resumeValues = null;
		return results;
	}
	
	/**
	 * Gets the Lua coroutine associated with the current thread, or null, if
	 * this thread does not represent a coroutine.
	 * @return Lua coroutine or null.
	 */
	public LuaCoroutine currentCoroutine() {
		return current.get();
	}
	
	private final Thread thread;
	
	private volatile Status status;
	private volatile Object[] resumeValues;
	private volatile Result yieldResult;
	private volatile Thread parent;
	private volatile boolean autoYield;
	
	private LuaCoroutine(Thread thread) {
		this.thread = thread;
		this.status = Status.SUSPENDED; // not started
	}
	
	public enum Status {
		/**
		 * Coroutine is currently executing.
		 */
		RUNNING,
		
		/**
		 * Coroutine has either not yet started, or has yielded.
		 */
		SUSPENDED,
		
		/**
		 * Coroutine has resumed another one, and is waiting for it to yield.
		 */
		NORMAL,
		
		/**
		 * Coroutine completed execution successfully or threw an exception.
		 */
		DEAD
	}
	
	/**
	 * Resumes this coroutine, allowing it to start or continue execution.
	 * @param values If this coroutine has not yet started, the values are
	 * given to main function of it as arguments. Otherwise, they are returned
	 * by its previous call to {@link #yield(Object...)}.
	 * @return Result of coroutine. This could be one of several things:
	 * 
	 * <ul>
	 * <li>If the coroutine completed execution successfully, the values
	 * values values returned by main function of it
	 * <li>If the coroutine threw an exception, that exception
	 * <li>If the coroutine yielded, whatever values that were given to
	 * {@link #yield(Object...)}
	 */
	public Result resume(Object... values) {
		return switch (status) {
		// Currently running coroutine could call resume on itself; this makes no sense, of course
		case RUNNING -> throw new LuaException("cannot resume non-suspended coroutine");
		case NORMAL -> throw new LuaException("cannot resume non-suspended coroutine");
		case DEAD -> throw new LuaException("cannot resume dead coroutine");
		case SUSPENDED -> {
			if (autoYield) {
				yield this.yieldResult;
			}
			
			// Update currently running coroutine (not 'this'!) if needed
			var caller = current.get();
			var child = this; // for clarity
			if (caller != null) {
				caller.status = Status.NORMAL; // Active, but waiting for another coroutine
			}
			
			// Start running 'this' coroutine
			child.resumeValues = values;
			child.parent = Thread.currentThread();
			child.status = Status.RUNNING;
			LockSupport.unpark(child.thread);
			
			// Park the caller until the child yields
			while (child.yieldResult == null) {
				LockSupport.park();
			}
			
			// When child yields or exits, resume the caller thread (which may be coroutine itself)
			if (caller != null) {
				caller.status = Status.RUNNING;
			}
			var results = child.yieldResult;
			if (!child.autoYield) {
				child.yieldResult = null;
			} // else: first resume from autoyield, do not clear results!
			yield results;
		}
		};
	}
	
}
