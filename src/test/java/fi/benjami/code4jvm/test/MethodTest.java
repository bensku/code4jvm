package fi.benjami.code4jvm.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import fi.benjami.code4jvm.ClassDef;
import fi.benjami.code4jvm.Constant;
import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.flag.Access;
import fi.benjami.code4jvm.statement.Return;

public class MethodTest {
	
	private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

	@Test
	public void empty() throws Throwable {
		var def = ClassDef.create("fi.benjami.code4jvm.test.EmptyMethod", Access.PUBLIC);
		def.addEmptyConstructor(Access.PUBLIC);
		
		var method = def.addMethod(Type.VOID, "doNothing", Access.PUBLIC);
		method.add(Return.nothing());
		
		var lookup = LOOKUP.defineHiddenClass(def.compile(), true);
		lookup.findVirtual(lookup.lookupClass(), "doNothing", MethodType.methodType(void.class))
				.invoke(TestUtils.newInstance(lookup));
	}
	
	@Test
	public void returning() throws Throwable {
		var def = ClassDef.create("fi.benjami.code4jvm.test.ReturningMethod", Access.PUBLIC);
		def.addEmptyConstructor(Access.PUBLIC);
		
		def.addMethod(Type.INT, "returnInt", Access.PUBLIC).add(Return.value(Constant.of(1337)));
		def.addMethod(Type.LONG, "returnLong", Access.PUBLIC).add(Return.value(Constant.of(42L)));
		def.addMethod(Type.of(String.class), "returnString", Access.PUBLIC).add(Return.value(Constant.of("hello")));
		
		var lookup = LOOKUP.defineHiddenClass(def.compile(), true);
		var instance = TestUtils.newInstance(lookup);
		assertEquals(1337, (int) lookup.findVirtual(lookup.lookupClass(), "returnInt", MethodType.methodType(int.class))
				.invoke(instance));
		assertEquals(42, (long) lookup.findVirtual(lookup.lookupClass(), "returnLong", MethodType.methodType(long.class))
				.invoke(instance));
		assertEquals("hello", (String) lookup.findVirtual(lookup.lookupClass(), "returnString", MethodType.methodType(String.class))
				.invoke(instance));
	}
	
	@Test
	public void withArguments() throws Throwable {
		var def = ClassDef.create("fi.benjami.code4jvm.test.WithArguments", Access.PUBLIC);
		def.addEmptyConstructor(Access.PUBLIC);
		
		var method = def.addMethod(Type.of(Object.class), "returnArg", Access.PUBLIC);
		var arg = method.arg(Type.of(Object.class));
		method.add(Return.value(arg));
		
		var lookup = LOOKUP.defineHiddenClass(def.compile(), true);
		var obj = new Object();
		assertEquals(obj, lookup.findVirtual(lookup.lookupClass(),
				"returnArg", MethodType.methodType(Object.class, Object.class))
				.invoke(TestUtils.newInstance(lookup), obj));
	}
	
	public record Constructable(
			String value
	) {}
	
	@Test
	public void constructor() throws Throwable {
		var def = ClassDef.create("fi.benjami.code4jvm.test.WithArguments", Access.PUBLIC);
		def.interfaces(Type.of(Supplier.class));
		def.addEmptyConstructor(Access.PUBLIC);
		
		var method = def.addMethod(Type.of(Object.class), "get", Access.PUBLIC);
		var obj = method.add(Type.of(Constructable.class).newInstance(Constant.of("ok"))).value();
		method.add(Return.value(obj));
		
		var lookup = LOOKUP.defineHiddenClass(def.compile(), true);
		var instance = (Supplier<?>) TestUtils.newInstance(lookup);
		assertEquals(new Constructable("ok"), instance.get());
	}
}
