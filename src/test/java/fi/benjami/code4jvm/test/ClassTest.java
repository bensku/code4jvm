package fi.benjami.code4jvm.test;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import org.junit.jupiter.api.Test;

import fi.benjami.code4jvm.ClassDef;
import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.flag.Access;
import fi.benjami.code4jvm.statement.Return;

public class ClassTest {

	private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
	
	@Test
	public void emptyClass() throws IllegalAccessException {
		var code = ClassDef.create("fi.benjami.code4jvm.test.EmptyClass", Access.PUBLIC).compile();
		LOOKUP.defineHiddenClass(code, true);
	}
	
	public class TestClass {}
	public interface TestInterface1 {}
	public interface TestInterface2 {}
	
	@Test
	public void extendingClass() throws IllegalAccessException {
		var def = ClassDef.create("fi.benjami.code4jvm.test.ExtendingClass", Access.PUBLIC);
		def.superClass(Type.of(TestClass.class));
		def.interfaces(Type.of(TestInterface1.class), Type.of(TestInterface2.class));
		LOOKUP.defineHiddenClass(def.compile(), true);
	}
	
	@Test
	public void withConstructor() throws Throwable {
		var def = ClassDef.create("fi.benjami.code4jvm.test.WithConstructor", Access.PUBLIC);
		var constructor = def.addConstructor(Access.PUBLIC);
		constructor.add(constructor.self().callSpecial(Type.OBJECT, Type.VOID, "<init>"));
		constructor.add(Return.nothing());
		
		var lookup = LOOKUP.defineHiddenClass(def.compile(), true);
		@SuppressWarnings("unused") // MethodHandles API doesn't like void return type
		var result = lookup.findConstructor(lookup.lookupClass(), MethodType.methodType(void.class)).invoke();
	}
	
}
