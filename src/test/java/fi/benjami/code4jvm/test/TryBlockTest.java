package fi.benjami.code4jvm.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.invoke.MethodHandles;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import fi.benjami.code4jvm.ClassDef;
import fi.benjami.code4jvm.Condition;
import fi.benjami.code4jvm.Constant;
import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.Value;
import fi.benjami.code4jvm.flag.Access;
import fi.benjami.code4jvm.statement.Return;
import fi.benjami.code4jvm.statement.Throw;
import fi.benjami.code4jvm.structure.IfBlock;
import fi.benjami.code4jvm.structure.TryBlock;

public class TryBlockTest {

	private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

	@Test
	public void noOpTry() throws Throwable {
		var def = ClassDef.create("fi.benjami.code4jvm.test.NoOpTry", Access.PUBLIC);
		def.addEmptyConstructor(Access.PUBLIC);
		def.interfaces(Type.of(Function.class));
		
		var method = def.addMethod(Type.OBJECT, "apply", Access.PUBLIC);
		var arg = method.arg(Type.OBJECT);
		method.add(TryBlock.create(block -> {
			block.add(Return.value(arg));
		}));
		method.add(Return.value(Constant.of("fail")));
		
		var lookup = LOOKUP.defineHiddenClass(def.compile(), true);
		@SuppressWarnings("unchecked")
		var instance = (Function<Object, Object>) TestUtils.newInstance(lookup);
		assertEquals("ok", instance.apply("ok"));
	}
	
	@Test
	public void tryCatch() throws Throwable {
		var def = ClassDef.create("fi.benjami.code4jvm.test.TryCatch", Access.PUBLIC);
		def.addEmptyConstructor(Access.PUBLIC);
		def.interfaces(Type.of(Function.class));
		
		var method = def.addMethod(Type.OBJECT, "apply", Access.PUBLIC);
		var arg = method.arg(Type.OBJECT);
		method.add(TryBlock.create(block -> {
			var exception = block.add(Type.of(UnsupportedOperationException.class)
					.newInstance(Constant.of("error msg"))).value();
			block.add(Throw.value(exception));
			block.add(Return.value(Constant.of("fail")));
		}).addCatch(Type.of(UnsupportedOperationException.class), (block, exception) -> {
			// Use the exception value to make sure it is correct
			var errorMsg = block.add(exception.callVirtual(Type.of(String.class), "getMessage")).value();
			block.add(new IfBlock().branch(Condition.equal(errorMsg, Constant.of("error msg")), inner -> {				
				inner.add(Return.value(arg));
			}));
		}));
		method.add(Return.value(Constant.of("fail")));
		
		var lookup = LOOKUP.defineHiddenClass(def.compile(), true);
		@SuppressWarnings("unchecked")
		var instance = (Function<Object, Object>) TestUtils.newInstance(lookup);
		assertEquals("ok", instance.apply("ok"));
	}
	
	@Test
	public void emptyExitHookReturn() throws Throwable {
		var def = ClassDef.create("fi.benjami.code4jvm.test.EmptyExitHookReturn", Access.PUBLIC);
		def.addEmptyConstructor(Access.PUBLIC);
		def.interfaces(Type.of(Function.class));
		
		var method = def.addMethod(Type.OBJECT, "apply", Access.PUBLIC);
		var arg = method.arg(Type.OBJECT);
		method.add(TryBlock.create(block -> {
			block.add(Return.value(arg));
		}).addExitHook(block -> {
			// No-op
		}));
		method.add(Return.value(Constant.of("fail")));
		
		var lookup = LOOKUP.defineHiddenClass(def.compile(), true);
		@SuppressWarnings("unchecked")
		var instance = (Function<Object, Object>) TestUtils.newInstance(lookup);
		assertEquals("ok", instance.apply("ok"));
	}
	
	@Test
	public void emptyExitHookThrow() throws Throwable {
		var def = ClassDef.create("fi.benjami.code4jvm.test.EmptyExitHookThrow", Access.PUBLIC);
		def.addEmptyConstructor(Access.PUBLIC);
		def.interfaces(Type.of(Function.class));
		
		var method = def.addMethod(Type.OBJECT, "apply", Access.PUBLIC);
		method.arg(Type.OBJECT);
		method.add(TryBlock.create(block -> {
			var exception = block.add(Type.of(UnsupportedOperationException.class)
					.newInstance(Constant.of("error msg"))).value();
			block.add(Throw.value(exception));
		}).addExitHook(block -> {
			// No-op
		}));
		method.add(Return.value(Constant.of("fail")));
		
		var lookup = LOOKUP.defineHiddenClass(def.compile(), true);
		@SuppressWarnings("unchecked")
		var instance = (Function<Object, Object>) TestUtils.newInstance(lookup);
		assertThrows(UnsupportedOperationException.class, () -> instance.apply("ok"));
	}
	
	@Test
	public void exitHookReturn() throws Throwable {
		var def = ClassDef.create("fi.benjami.code4jvm.test.ExitHookReturn", Access.PUBLIC);
		def.addEmptyConstructor(Access.PUBLIC);
		def.interfaces(Type.of(Function.class));
		
		var method = def.addMethod(Type.OBJECT, "apply", Access.PUBLIC);
		var arg = method.arg(Type.OBJECT);
		method.add(TryBlock.create(block -> {
			block.add(Return.value(Constant.of("fail")));
		}).addExitHook(block -> {
			block.add(Return.value(arg));
		}));
		method.add(Return.value(Constant.of("fail")));
		
		var lookup = LOOKUP.defineHiddenClass(def.compile(), true);
		@SuppressWarnings("unchecked")
		var instance = (Function<Object, Object>) TestUtils.newInstance(lookup);
		assertEquals("ok", instance.apply("ok"));
	}
	
	@Test
	public void exitHookThrow() throws Throwable {
		var def = ClassDef.create("fi.benjami.code4jvm.test.ExitHookThrow", Access.PUBLIC);
		def.addEmptyConstructor(Access.PUBLIC);
		def.interfaces(Type.of(Function.class));
		
		var method = def.addMethod(Type.OBJECT, "apply", Access.PUBLIC);
		var arg = method.arg(Type.OBJECT);
		method.add(TryBlock.create(block -> {
			var exception = block.add(Type.of(UnsupportedOperationException.class)
					.newInstance(Constant.of("error msg"))).value();
			block.add(Throw.value(exception));
		}).addExitHook(block -> {
			block.add(Return.value(arg));
		}));
		method.add(Return.value(Constant.of("fail")));
		
		var lookup = LOOKUP.defineHiddenClass(def.compile(), true);
		@SuppressWarnings("unchecked")
		var instance = (Function<Object, Object>) TestUtils.newInstance(lookup);
		assertEquals("ok", instance.apply("ok"));
	}
	
	@Test
	public void emptyReturnHook() throws Throwable {
		var def = ClassDef.create("fi.benjami.code4jvm.test.EmptyReturnHook", Access.PUBLIC);
		def.addEmptyConstructor(Access.PUBLIC);
		def.interfaces(Type.of(Function.class));
		
		var method = def.addMethod(Type.OBJECT, "apply", Access.PUBLIC);
		var arg = method.arg(Type.OBJECT);
		method.add(TryBlock.create(block -> {
			var variable = block.add(Value.uninitialized(Type.of(String.class))).variable();
			block.add(variable.set(Constant.of("fail")));
			block.add(Return.value(variable));
		}).addReturnHook((block, value) -> {
			// Suppress return
		}));
		method.add(Return.value(arg));
		
		var lookup = LOOKUP.defineHiddenClass(def.compile(), true);
		@SuppressWarnings("unchecked")
		var instance = (Function<Object, Object>) TestUtils.newInstance(lookup);
		assertEquals("ok", instance.apply("ok"));
	}
	
	@Test
	public void returnFromHook() throws Throwable {
		var def = ClassDef.create("fi.benjami.code4jvm.test.ReturnHookReturn", Access.PUBLIC);
		def.addEmptyConstructor(Access.PUBLIC);
		def.interfaces(Type.of(Function.class));
		
		var method = def.addMethod(Type.OBJECT, "apply", Access.PUBLIC);
		var arg = method.arg(Type.OBJECT);
		method.add(TryBlock.create(block -> {
			block.add(Return.value(arg));
		}).addReturnHook((block, value) -> {
			block.add(Return.value(value));
		}));
		method.add(Return.value(Constant.of("fail")));
		
		var lookup = LOOKUP.defineHiddenClass(def.compile(), true);
		@SuppressWarnings("unchecked")
		var instance = (Function<Object, Object>) TestUtils.newInstance(lookup);
		assertEquals("ok", instance.apply("ok"));
	}
	
	@Test
	public void emptyThrowHook() throws Throwable {
		var def = ClassDef.create("fi.benjami.code4jvm.test.EmptyThrowHook", Access.PUBLIC);
		def.addEmptyConstructor(Access.PUBLIC);
		def.interfaces(Type.of(Function.class));
		
		var method = def.addMethod(Type.OBJECT, "apply", Access.PUBLIC);
		var arg = method.arg(Type.OBJECT);
		method.add(TryBlock.create(block -> {
			var exception = block.add(Type.of(UnsupportedOperationException.class)
					.newInstance(Constant.of("error msg"))).value();
			block.add(Throw.value(exception));
		}).addThrowHook((block, value) -> {
			// No-op
		}));
		method.add(Return.value(arg));
		
		var lookup = LOOKUP.defineHiddenClass(def.compile(), true);
		@SuppressWarnings("unchecked")
		var instance = (Function<Object, Object>) TestUtils.newInstance(lookup);
		assertEquals("ok", instance.apply("ok"));
	}
	
	@Test
	public void rethrowFromHook() throws Throwable {
		var def = ClassDef.create("fi.benjami.code4jvm.test.RethrowFromHook", Access.PUBLIC);
		def.addEmptyConstructor(Access.PUBLIC);
		def.interfaces(Type.of(Function.class));
		
		var method = def.addMethod(Type.OBJECT, "apply", Access.PUBLIC);
		method.arg(Type.OBJECT);
		method.add(TryBlock.create(block -> {
			var exception = block.add(Type.of(UnsupportedOperationException.class)
					.newInstance(Constant.of("error msg"))).value();
			block.add(Throw.value(exception));
		}).addThrowHook((block, value) -> {
			block.add(Throw.value(value));
		}));
		method.add(Return.value(Constant.of("fail")));
		
		var lookup = LOOKUP.defineHiddenClass(def.compile(), true);
		@SuppressWarnings("unchecked")
		var instance = (Function<Object, Object>) TestUtils.newInstance(lookup);
		assertThrows(UnsupportedOperationException.class, () -> instance.apply("ok"));
	}
	
	@Test
	public void returnFromCatch() throws Throwable {
		var def = ClassDef.create("fi.benjami.code4jvm.test.ReturnFromCatch", Access.PUBLIC);
		def.addEmptyConstructor(Access.PUBLIC);
		def.interfaces(Type.of(Function.class));
		
		var method = def.addMethod(Type.OBJECT, "apply", Access.PUBLIC);
		var arg = method.arg(Type.OBJECT);
		method.add(TryBlock.create(block -> {
			var exception = block.add(Type.of(UnsupportedOperationException.class)
					.newInstance(Constant.of("error msg"))).value();
			block.add(Throw.value(exception));
			block.add(Return.value(Constant.of("fail")));
		}).addCatch(Type.of(UnsupportedOperationException.class), (block, exception) -> {
			block.add(Return.value(Constant.of("fail")));
		}).addReturnHook((block, value) -> {
			// No-op, fall through
		}));
		method.add(Return.value(arg));
		
		var lookup = LOOKUP.defineHiddenClass(def.compile(), true);
		@SuppressWarnings("unchecked")
		var instance = (Function<Object, Object>) TestUtils.newInstance(lookup);
		assertEquals("ok", instance.apply("ok"));
	}
	
	@Test
	public void throwFromCatch() throws Throwable {
		var def = ClassDef.create("fi.benjami.code4jvm.test.ThrowFromCatch", Access.PUBLIC);
		def.addEmptyConstructor(Access.PUBLIC);
		def.interfaces(Type.of(Function.class));
		
		var method = def.addMethod(Type.OBJECT, "apply", Access.PUBLIC);
		var arg = method.arg(Type.OBJECT);
		method.add(TryBlock.create(block -> {
			var exception = block.add(Type.of(UnsupportedOperationException.class)
					.newInstance(Constant.of("error msg"))).value();
			block.add(Throw.value(exception));
			block.add(Return.value(Constant.of("fail")));
		}).addCatch(Type.of(UnsupportedOperationException.class), (block, exception) -> {
			var exception2 = block.add(Type.of(IllegalStateException.class)
					.newInstance(Constant.of("error msg"))).value();
			block.add(Throw.value(exception2));
		}).addThrowHook((block, value) -> {
			// No-op, fall through
		}).addReturnHook((block, value) -> {
			// Should not run
			block.add(Return.value(Constant.of("fail")));
		}));
		method.add(Return.value(arg));
		
		var lookup = LOOKUP.defineHiddenClass(def.compile(), true);
		@SuppressWarnings("unchecked")
		var instance = (Function<Object, Object>) TestUtils.newInstance(lookup);
		assertEquals("ok", instance.apply("ok"));
	}
}
