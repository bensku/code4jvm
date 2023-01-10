package fi.benjami.code4jvm.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;

import fi.benjami.code4jvm.Constant;
import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.config.CompileOptions;
import fi.benjami.code4jvm.flag.Access;
import fi.benjami.code4jvm.flag.FieldFlag;
import fi.benjami.code4jvm.statement.Return;
import fi.benjami.code4jvm.typedef.ClassDef;

@ExtendWith({EnableDebugExtension.class})
public class FieldTest {
	
	interface Storage {
		Object get();
		void set(Object value);
	}
	
	@ParameterizedTest
	@OptionsSource
	public void instanceField(CompileOptions opts) throws Throwable {
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
		
		var instance = (Storage) TestUtils.newInstance(def, opts);
		assertNull(instance.get());
		var obj = new Object();
		instance.set(obj);
		assertEquals(obj, instance.get());
	}
	
	@ParameterizedTest
	@OptionsSource
	public void staticField(CompileOptions opts) throws Throwable {
		var def = ClassDef.create("fi.benjami.code4jvm.test.StaticField", Access.PUBLIC);
		def.interfaces(Type.of(Storage.class));
		
		def.addStaticField(Access.PUBLIC, Type.OBJECT, "first");
		def.addStaticField(Access.PUBLIC, "second", Constant.of("foo"));
		
		var lookup = TestUtils.loadHidden(def, opts);
		var first = lookup.lookupClass().getField("first");
		assertNull(first.get(null));
		var obj = new Object();
		first.set(null, obj);
		assertEquals(obj, first.get(null));
		
		var second = lookup.lookupClass().getField("second");
		assertEquals("foo", second.get(null));
	}
	
	@ParameterizedTest
	@OptionsSource
	public void finalStaticField(CompileOptions opts) throws Throwable {
		var def = ClassDef.create("fi.benjami.code4jvm.test.FinalStaticField", Access.PUBLIC);
		def.interfaces(Type.of(Storage.class));
		
		def.addStaticField(Access.PUBLIC, "finalField", Constant.of("foo"), FieldFlag.FINAL);
		
		var lookup = TestUtils.loadHidden(def, opts);
		var field = lookup.lookupClass().getField("finalField");
		assertEquals("foo", field.get(null));
		assertThrows(IllegalAccessException.class, () -> field.set(null, "bar"));
	}
}
