package fi.benjami.code4jvm.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.invoke.MethodHandles;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;

import fi.benjami.code4jvm.Block;
import fi.benjami.code4jvm.ClassDef;
import fi.benjami.code4jvm.Condition;
import fi.benjami.code4jvm.Constant;
import fi.benjami.code4jvm.flag.Access;
import fi.benjami.code4jvm.statement.Jump;
import fi.benjami.code4jvm.statement.Return;

public class BlockTest {

	private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

	@Test
	public void simpleBlock() throws Throwable {
		var def = ClassDef.create("fi.benjami.code4jvm.test.SimpleBlock", Access.PUBLIC);
		def.addEmptyConstructor(Access.PUBLIC);
		def.interfaces(Type.getType(Supplier.class));
		
		var method = def.addMethod(Type.getType(Object.class), "get", Access.PUBLIC);
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
		def.interfaces(Type.getType(Supplier.class));
		
		var method = def.addMethod(Type.getType(Object.class), "get", Access.PUBLIC);
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
		def.interfaces(Type.getType(Supplier.class));
		
		var method = def.addMethod(Type.getType(Object.class), "get", Access.PUBLIC);
		var block = Block.create();
		block.add(Jump.to(block, Jump.Target.END, Condition.isTrue(Constant.of(true))));
		block.add(Return.value(Constant.of("not ok")));
		method.add(block);
		method.add(Return.value(Constant.of("ok")));
		
		var lookup = LOOKUP.defineHiddenClass(def.compile(), true);
		var instance = (Supplier<?>) TestUtils.newInstance(lookup);
		assertEquals("ok", instance.get());
	}
	
}
