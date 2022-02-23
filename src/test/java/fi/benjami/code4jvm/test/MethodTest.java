package fi.benjami.code4jvm.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;

import fi.benjami.code4jvm.ClassDef;
import fi.benjami.code4jvm.Constant;
import fi.benjami.code4jvm.flag.Access;
import fi.benjami.code4jvm.statement.Return;

public class MethodTest {
	
	private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
	
	private Object newInstance(MethodHandles.Lookup lookup) throws Throwable {
		return lookup.findConstructor(lookup.lookupClass(), MethodType.methodType(void.class)).invoke();
	}

	@Test
	public void emptyMethod() throws Throwable {
		var def = ClassDef.create("fi.benjami.code4jvm.test.EmptyMethod", Access.PUBLIC);
		def.addEmptyConstructor(Access.PUBLIC);
		
		var method = def.addMethod(Type.VOID_TYPE, "doNothing", Access.PUBLIC);
		method.add(Return.nothing());
		
		var lookup = LOOKUP.defineHiddenClass(def.compile(), true);
		lookup.findVirtual(lookup.lookupClass(), "doNothing", MethodType.methodType(void.class))
				.invoke(newInstance(lookup));
	}
	
	@Test
	public void returningMethod() throws Throwable {
		var def = ClassDef.create("fi.benjami.code4jvm.test.ReturningMethod", Access.PUBLIC);
		def.addEmptyConstructor(Access.PUBLIC);
		
		def.addMethod(Type.INT_TYPE, "returnInt", Access.PUBLIC).add(Return.value(Constant.of(1337)));
		def.addMethod(Type.LONG_TYPE, "returnLong", Access.PUBLIC).add(Return.value(Constant.of(42L)));
		def.addMethod(Type.getType(String.class), "returnString", Access.PUBLIC).add(Return.value(Constant.of("hello")));
		
		var lookup = LOOKUP.defineHiddenClass(def.compile(), true);
		assertEquals(1337, (int) lookup.findVirtual(lookup.lookupClass(), "returnInt", MethodType.methodType(int.class))
				.invoke(newInstance(lookup)));
		assertEquals(42, (long) lookup.findVirtual(lookup.lookupClass(), "returnLong", MethodType.methodType(long.class))
				.invoke(newInstance(lookup)));
		assertEquals("hello", (String) lookup.findVirtual(lookup.lookupClass(), "returnString", MethodType.methodType(String.class))
				.invoke(newInstance(lookup)));
	}
}
