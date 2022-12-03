package fi.benjami.code4jvm.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.flag.Access;
import fi.benjami.code4jvm.flag.ClassFlag;
import fi.benjami.code4jvm.flag.FieldFlag;
import fi.benjami.code4jvm.statement.Return;
import fi.benjami.code4jvm.typedef.ClassDef;

@ExtendWith({EnableDebugExtension.class})
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
		constructor.add(constructor.self().callPrivate(Type.OBJECT, Type.VOID, "<init>"));
		constructor.add(Return.nothing());
		
		var lookup = LOOKUP.defineHiddenClass(def.compile(), true);
		@SuppressWarnings("unused") // MethodHandles API doesn't like void return type
		var result = lookup.findConstructor(lookup.lookupClass(), MethodType.methodType(void.class)).invoke();
	}
	
	@Test
	public void abstractClass() throws Throwable {
		var def = ClassDef.create("fi.benjami.code4jvm.test.AbstractClass", Access.PUBLIC, ClassFlag.ABSTRACT);
		var method = def.addAbstractMethod(Type.VOID, "doStuff", Access.PUBLIC);
		method.arg(Type.OBJECT);
		
		var lookup = LOOKUP.defineHiddenClass(def.compile(), true);
		lookup.lookupClass().getMethod("doStuff", Object.class);
	}
	
	@Test
	public void abstractMethodMisuse() {
		var def = ClassDef.create("fi.benjami.code4jvm.test.AbstractMethodMisuse", Access.PUBLIC);
		assertThrows(IllegalArgumentException.class, () -> def.addAbstractMethod(Type.VOID, "doStuff", Access.PUBLIC));
	}
	
	@Test
	public void interfaceClass() throws Throwable {
		var def = ClassDef.create("fi.benjami.code4jvm.test.InterfaceClass", Access.PUBLIC, ClassFlag.INTERFACE);
		{			
			var method = def.addAbstractMethod(Type.VOID, "abstractMethod", Access.PUBLIC);
			method.arg(Type.OBJECT);
		}
		{
			var method = def.addMethod(Type.OBJECT, "defaultMethod", Access.PUBLIC);
			var arg = method.arg(Type.OBJECT);
			method.add(Return.value(arg));
		}
		{
			var method = def.addStaticMethod(Type.OBJECT, "publicStatic", Access.PUBLIC);
			var arg = method.arg(Type.OBJECT);
			var value = method.add(def.type().callStatic(Type.OBJECT, "privateStatic", arg));
			method.add(Return.value(value));
		}
		{
			var method = def.addStaticMethod(Type.OBJECT, "privateStatic", Access.PRIVATE);
			var arg = method.arg(Type.OBJECT);
			method.add(Return.value(arg));
		}
		assertThrows(IllegalArgumentException.class, () -> def.addConstructor(Access.PUBLIC));
		assertThrows(IllegalArgumentException.class, () -> def.addStaticMethod(Type.OBJECT, "disallowedAccess", Access.PACKAGE_PRIVATE));
		assertThrows(IllegalArgumentException.class, () -> def.addMethod(Type.OBJECT, "privateInstance", Access.PRIVATE));
		
		def.addStaticField(Access.PUBLIC, Type.OBJECT, "staticField", FieldFlag.FINAL);
		assertThrows(IllegalArgumentException.class, () -> def.addStaticField(Access.PUBLIC, Type.OBJECT, "staticNotFinal"));
		assertThrows(IllegalArgumentException.class, () -> def.addField(false, Access.PUBLIC, Type.OBJECT, "instanceField"));
		
		var lookup = LOOKUP.defineHiddenClass(def.compile(), true);
		lookup.lookupClass().getMethod("abstractMethod", Object.class);
		var obj = new Object();
		assertEquals(obj, lookup.lookupClass().getMethod("publicStatic", Object.class).invoke(null, obj));
		
		lookup.lookupClass().getField("staticField");
	}
	
}
