package fi.benjami.code4jvm.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.function.IntSupplier;
import java.util.function.Supplier;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;

import fi.benjami.code4jvm.Condition;
import fi.benjami.code4jvm.Constant;
import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.Variable;
import fi.benjami.code4jvm.block.Block;
import fi.benjami.code4jvm.block.ReturnRedirect;
import fi.benjami.code4jvm.config.CompileOptions;
import fi.benjami.code4jvm.flag.Access;
import fi.benjami.code4jvm.statement.Arithmetic;
import fi.benjami.code4jvm.statement.Jump;
import fi.benjami.code4jvm.statement.Return;
import fi.benjami.code4jvm.typedef.ClassDef;

@ExtendWith({EnableDebugExtension.class})
public class BlockTest {

	@ParameterizedTest
	@OptionsSource
	public void simpleBlock(CompileOptions opts) throws Throwable {
		var def = ClassDef.create("fi.benjami.code4jvm.test.SimpleBlock", Access.PUBLIC);
		def.addEmptyConstructor(Access.PUBLIC);
		def.interfaces(Type.of(Supplier.class));
		
		var method = def.addMethod(Type.OBJECT, "get", Access.PUBLIC);
		var block = Block.create();
		block.add(Return.value(Constant.of("ok")));
		method.add(block);
		method.add(Return.nothing()); // Verifier should error if this is reachable
		
		var instance = (Supplier<?>) TestUtils.newInstance(def, opts);
		assertEquals("ok", instance.get());
	}
	
	@ParameterizedTest
	@OptionsSource
	public void jumpToEnd(CompileOptions opts) throws Throwable {
		var def = ClassDef.create("fi.benjami.code4jvm.test.JumpToEnd", Access.PUBLIC);
		def.addEmptyConstructor(Access.PUBLIC);
		def.interfaces(Type.of(Supplier.class));
		
		var method = def.addMethod(Type.OBJECT, "get", Access.PUBLIC);
		var block = Block.create();
		block.add(Jump.to(block, Jump.Target.END));
		block.add(Return.nothing()); // Verifier error if reachable
		method.add(block);
		method.add(Return.value(Constant.of("ok")));
		
		var instance = (Supplier<?>) TestUtils.newInstance(def, opts);
		assertEquals("ok", instance.get());
	}
	
	@ParameterizedTest
	@OptionsSource
	public void conditionalJump(CompileOptions opts) throws Throwable {
		var def = ClassDef.create("fi.benjami.code4jvm.test.JumpToEnd", Access.PUBLIC);
		def.addEmptyConstructor(Access.PUBLIC);
		def.interfaces(Type.of(Supplier.class));
		
		var method = def.addMethod(Type.OBJECT, "get", Access.PUBLIC);
		var block = Block.create();
		block.add(Jump.to(block, Jump.Target.END, Condition.isTrue(Constant.of(true))));
		block.add(Return.value(Constant.of("not ok")));
		method.add(block);
		method.add(Return.value(Constant.of("ok")));
		
		var instance = (Supplier<?>) TestUtils.newInstance(def, opts);
		assertEquals("ok", instance.get());
	}
	
	@ParameterizedTest
	@OptionsSource
	public void simpleLoop(CompileOptions opts) throws Throwable {
		var def = ClassDef.create("fi.benjami.code4jvm.test.SimpleLoop", Access.PUBLIC);
		def.addEmptyConstructor(Access.PUBLIC);
		def.interfaces(Type.of(IntSupplier.class));
		
		var method = def.addMethod(Type.INT, "getAsInt", Access.PUBLIC);
		var counter = method.add("counter", Constant.of(0).copy());
		var loop = Block.create();
		loop.add(counter, Arithmetic.add(counter, Constant.of(1)));
		loop.add(Jump.to(loop, Jump.Target.START, Condition.lessThan(counter, Constant.of(100))));
		method.add(loop);
		method.add(Return.value(counter));
		
		var instance = (IntSupplier) TestUtils.newInstance(def, opts);
		assertEquals(100, instance.getAsInt());
	}
	
	@ParameterizedTest
	@OptionsSource
	public void blockAddedTwice(CompileOptions opts) throws Throwable {
		var def = ClassDef.create("fi.benjami.code4jvm.test.BlockAddedTwice", Access.PUBLIC);
		def.interfaces(Type.of(Supplier.class));
		def.addEmptyConstructor(Access.PUBLIC);
		
		var method = def.addMethod(Type.of(Object.class), "get", Access.PUBLIC);
		var a = Block.create();
		method.add(a);
		assertThrows(IllegalArgumentException.class, () -> method.add(a));
	}
	
	@ParameterizedTest
	@OptionsSource
	public void rawReturnRedirect(CompileOptions opts) throws Throwable {
		// See also TryBlockTest for high-level API tests
		var def = ClassDef.create("fi.benjami.code4jvm.test.RawReturnRedirect", Access.PUBLIC);
		def.interfaces(Type.of(Supplier.class));
		def.addEmptyConstructor(Access.PUBLIC);
		
		var method = def.addMethod(Type.of(Object.class), "get", Access.PUBLIC);
		
		var block = Block.create();
		block.add(Return.value(Constant.of("ok")));
		var endBlock = Block.create();
		endBlock.add(Return.value(Constant.of("ok")));
		
		var handler = Block.create();
		var capturedReturn = Variable.create(Type.METHOD_RETURN_TYPE);
		handler.add(Jump.to(endBlock, Jump.Target.START, Condition.equal(capturedReturn.asType(Type.STRING),
				Constant.of("ok"))));
		handler.add(Return.value(Constant.of("fail")));
		
		block.setReturnRedirect(new ReturnRedirect(handler, capturedReturn));
		
		method.add(block);
		method.add(handler);
		method.add(endBlock);
		
		var instance = (Supplier<?>) TestUtils.newInstance(def, opts);
		assertEquals("ok", instance.get());
	}
	
}
