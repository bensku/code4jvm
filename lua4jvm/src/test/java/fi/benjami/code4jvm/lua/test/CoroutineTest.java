package fi.benjami.code4jvm.lua.test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

import org.junit.jupiter.api.Test;

import fi.benjami.code4jvm.lua.runtime.LuaCoroutine;
import fi.benjami.code4jvm.lua.stdlib.LuaException;

public class CoroutineTest {

	@Test
	public void simpleReturn() {
		var coro = LuaCoroutine.create(args -> args);
		var result = coro.resume("foo", "bar", "baz");
		assertTrue(result.success());
		assertArrayEquals(new Object[] {"foo", "bar", "baz"}, result.values());
	}
	
	@Test
	public void yieldAndReturn() {
		var resumeVals = new AtomicReference<Object[]>();
		
		var coro = LuaCoroutine.create(args -> {
			resumeVals.set(LuaCoroutine.yield(args));
			return new Object[] {"return!"};
		});
		var result = coro.resume("foo", "bar", "baz");
		assertTrue(result.success());
		assertArrayEquals(new Object[] {"foo", "bar", "baz"}, result.values());
		
		result = coro.resume("foo", "bar");
		assertArrayEquals(new Object[] {"foo", "bar"}, resumeVals.get());
		assertTrue(result.success());
		assertArrayEquals(new Object[] {"return!"}, result.values());
	}
	
	@Test
	public void coroutineFailure() {
		var resumeVals = new AtomicReference<Object[]>();
		
		var coro = LuaCoroutine.create(args -> {
			resumeVals.set(LuaCoroutine.yield(args));
			throw new LuaException("error");
		});
		var result = coro.resume("foo", "bar", "baz");
		assertTrue(result.success());
		assertArrayEquals(new Object[] {"foo", "bar", "baz"}, result.values());
		
		result = coro.resume("foo", "bar", "baz");
		assertArrayEquals(new Object[] {"foo", "bar", "baz"}, resumeVals.get());
		assertFalse(result.success());
		var error = (LuaException) result.values()[0];
		assertEquals("error", error.getLuaMessage());
	}
	
	@Test
	public void detachThread() {
		var coroLatch = new CountDownLatch(1);
		var coro = LuaCoroutine.create(args -> {
			try (var ticket = LuaCoroutine.detachYield(args)) {
				try {
					coroLatch.await();
				} catch (InterruptedException e) {
					throw new AssertionError(e);
				}
			}
			return new Object[] {"completed!"};
		});
		
		var args = new Object[] {"a", "b", "c", "test"};
		assertEquals(args, coro.resume(args).values());
		for (var i = 0; i < 10; i++) {
			assertEquals(args, coro.resume("another", "value").values());
		}
		
		coroLatch.countDown();
		
		// It might take a while for the coroutine thread to actually "attach" back
		for (var i = 0; i < 100; i++) {
			LockSupport.parkNanos(500_000_000);
			var res = coro.resume("whatever");
			assertTrue(res.success());
			if (res.values() == args) {
				continue;
			}
			assertArrayEquals(new Object[] {"completed!"}, res.values());
			break;
		}
	}
}
