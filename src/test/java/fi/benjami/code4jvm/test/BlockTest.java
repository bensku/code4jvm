package fi.benjami.code4jvm.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.invoke.MethodHandles;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import fi.benjami.code4jvm.ClassDef;
import fi.benjami.code4jvm.Condition;
import fi.benjami.code4jvm.Constant;
import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.Variable;
import fi.benjami.code4jvm.block.Block;
import fi.benjami.code4jvm.block.ReturnRedirect;
import fi.benjami.code4jvm.flag.Access;
import fi.benjami.code4jvm.statement.Arithmetic;
import fi.benjami.code4jvm.statement.Jump;
import fi.benjami.code4jvm.statement.Return;

public class BlockTest {

	private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

	@Test
	public void simpleBlock() throws Throwable {
		var def = ClassDef.create("fi.benjami.code4jvm.test.SimpleBlock", Access.PUBLIC);
		def.addEmptyConstructor(Access.PUBLIC);
		def.interfaces(Type.of(Supplier.class));
		
		var method = def.addMethod(Type.OBJECT, "get", Access.PUBLIC);
		var block = Block.create();
		block.add(Return.value(Constant.of("ok")));
		method.add(block);
		method.add(Return.nothing()); // Verifier should error if this is reachable
		
		var lookup = LOOKUP.defineHiddenClass(def.compile(), true);
		var instance = (Supplier<?>) TestUtils.newInstance(lookup);
		assertEquals("ok", instance.get());
	}
	
	@Test
	public void jumpToEnd() throws Throwable {
		var def = ClassDef.create("fi.benjami.code4jvm.test.JumpToEnd", Access.PUBLIC);
		def.addEmptyConstructor(Access.PUBLIC);
		def.interfaces(Type.of(Supplier.class));
		
		var method = def.addMethod(Type.OBJECT, "get", Access.PUBLIC);
		var block = Block.create();
		block.add(Jump.to(block, Jump.Target.END));
		block.add(Return.nothing()); // Verifier error if reachable
		method.add(block);
		method.add(Return.value(Constant.of("ok")));
		
		var lookup = LOOKUP.defineHiddenClass(def.compile(), true);
		var instance = (Supplier<?>) TestUtils.newInstance(lookup);
		assertEquals("ok", instance.get());
	}
	
	@Test
	public void conditionalJump() throws Throwable {
		var def = ClassDef.create("fi.benjami.code4jvm.test.JumpToEnd", Access.PUBLIC);
		def.addEmptyConstructor(Access.PUBLIC);
		def.interfaces(Type.of(Supplier.class));
		
		var method = def.addMethod(Type.OBJECT, "get", Access.PUBLIC);
		var block = Block.create();
		block.add(Jump.to(block, Jump.Target.END, Condition.isTrue(Constant.of(true))));
		block.add(Return.value(Constant.of("not ok")));
		method.add(block);
		method.add(Return.value(Constant.of("ok")));
		
		var lookup = LOOKUP.defineHiddenClass(def.compile(), true);
		var instance = (Supplier<?>) TestUtils.newInstance(lookup);
		assertEquals("ok", instance.get());
	}
	
	@Test
	public void simpleLoop() throws Throwable {
		var def = ClassDef.create("fi.benjami.code4jvm.test.SimpleLoop", Access.PUBLIC);
		def.addEmptyConstructor(Access.PUBLIC);
		def.interfaces(Type.of(IntSupplier.class));
		
		var method = def.addMethod(Type.INT, "getAsInt", Access.PUBLIC);
		var counter = method.add(Constant.of(0).copy()).variable("counter");
		var loop = Block.create();
		var next = loop.add(Arithmetic.add(counter, Constant.of(1))).value();
		loop.add(counter.set(next));
		loop.add(Jump.to(loop, Jump.Target.START, Condition.lessThan(counter, Constant.of(100))));
		method.add(loop);
		method.add(Return.value(counter));
		
		var lookup = LOOKUP.defineHiddenClass(def.compile(), true);
		var instance = (IntSupplier) TestUtils.newInstance(lookup);
		assertEquals(100, instance.getAsInt());
	}
	
	@Test
	public void blockAddedTwice() throws Throwable {
		var def = ClassDef.create("fi.benjami.code4jvm.test.BlockAddedTwice", Access.PUBLIC);
		def.interfaces(Type.of(Supplier.class));
		def.addEmptyConstructor(Access.PUBLIC);
		
		var method = def.addMethod(Type.of(Object.class), "get", Access.PUBLIC);
		var a = Block.create();
		method.add(a);
		assertThrows(IllegalArgumentException.class, () -> method.add(a));
	}
	
	@Test
	public void rawReturnRedirect() throws Throwable {
		// See also TryBlockTest for high-level API tests
		var def = ClassDef.create("fi.benjami.code4jvm.test.RawReturnRedirect", Access.PUBLIC);
		def.interfaces(Type.of(Supplier.class));
		def.addEmptyConstructor(Access.PUBLIC);
		
		var method = def.addMethod(Type.of(Object.class), "get", Access.PUBLIC);
		
		var block = Block.create();
		block.add(Return.value(Constant.of("ok")));
		var endBlock = block.copy();
		
		var handler = Block.create();
		var capturedReturn = Variable.createUnbound(Type.METHOD_RETURN_TYPE);
		handler.add(Jump.to(endBlock, Jump.Target.START, Condition.equal(capturedReturn.asType(Type.of(String.class)),
				Constant.of("ok"))));
		handler.add(Return.value(Constant.of("fail")));
		
		block.setReturnRedirect(new ReturnRedirect(handler, capturedReturn));
		
		method.add(block);
		method.add(handler);
		method.add(endBlock);
		
		var lookup = LOOKUP.defineHiddenClass(def.compile(), true);
		var instance = (Supplier<?>) TestUtils.newInstance(lookup);
		assertEquals("ok", instance.get());
	}
	
}
