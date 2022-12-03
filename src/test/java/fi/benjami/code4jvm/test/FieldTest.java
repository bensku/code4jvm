package fi.benjami.code4jvm.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.invoke.MethodHandles;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import fi.benjami.code4jvm.Constant;
import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.flag.Access;
import fi.benjami.code4jvm.flag.FieldFlag;
import fi.benjami.code4jvm.statement.Return;
import fi.benjami.code4jvm.typedef.ClassDef;

@ExtendWith({EnableDebugExtension.class})
public class FieldTest {
	
	private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

	interface Storage {
		Object get();
		void set(Object value);
	}
	
	@Test
	public void instanceField() throws Throwable {
		var def = ClassDef.create("fi.benjami.code4jvm.test.InstanceField", Access.PUBLIC);
		def.interfaces(Type.of(Storage.class));
		def.addEmptyConstructor(Access.PUBLIC);
		
		def.addInstanceField(Access.PRIVATE, Type.OBJECT, "data");
		
		var getter = def.addMethod(Type.OBJECT, "get", Access.PUBLIC);
		var storedValue = getter.add(getter.self().getField(Type.OBJECT, "data"));
		getter.add(Return.value(storedValue));
		
		var setter = def.addMethod(Type.VOID, "set", Access.PUBLIC);
		var newValue = setter.arg(Type.OBJECT);
		setter.add(setter.self().putField("data", newValue));
		setter.add(Return.nothing());
		
		var lookup = LOOKUP.defineHiddenClass(def.compile(), true);
		var instance = (Storage) TestUtils.newInstance(lookup);
		assertNull(instance.get());
		var obj = new Object();
		instance.set(obj);
		assertEquals(obj, instance.get());
	}
	
	@Test
	public void staticField() throws Throwable {
		var def = ClassDef.create("fi.benjami.code4jvm.test.StaticField", Access.PUBLIC);
		def.interfaces(Type.of(Storage.class));
		
		def.addStaticField(Access.PUBLIC, Type.OBJECT, "first");
		def.addStaticField(Access.PUBLIC, "second", Constant.of("foo"));
		
		var lookup = LOOKUP.defineHiddenClass(def.compile(), true);
		var first = lookup.lookupClass().getField("first");
		assertNull(first.get(null));
		var obj = new Object();
		first.set(null, obj);
		assertEquals(obj, first.get(null));
		
		var second = lookup.lookupClass().getField("second");
		assertEquals("foo", second.get(null));
	}
	
	@Test
	public void finalStaticField() throws Throwable {
		var def = ClassDef.create("fi.benjami.code4jvm.test.FinalStaticField", Access.PUBLIC);
		def.interfaces(Type.of(Storage.class));
		
		def.addStaticField(Access.PUBLIC, "finalField", Constant.of("foo"), FieldFlag.FINAL);
		
		var lookup = LOOKUP.defineHiddenClass(def.compile(), true);
		var field = lookup.lookupClass().getField("finalField");
		assertEquals("foo", field.get(null));
		assertThrows(IllegalAccessException.class, () -> field.set(null, "bar"));
	}
}
