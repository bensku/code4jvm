package fi.benjami.code4jvm.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Array;

import org.junit.jupiter.api.Test;

import fi.benjami.code4jvm.Constant;
import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.Value;
import fi.benjami.code4jvm.flag.Access;
import fi.benjami.code4jvm.flag.FieldFlag;
import fi.benjami.code4jvm.statement.Arithmetic;
import fi.benjami.code4jvm.statement.Return;
import fi.benjami.code4jvm.typedef.EnumDef;

public class EnumTest {

	public static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
	
	// Note: hidden classes cannot be used here, because enums reference themself
	// (e.g. $VALUES array)
	
	@Test
	public void emptyEnum() throws Throwable {
		var def = EnumDef.create("fi.benjami.code4jvm.test.EmptyEnum", Access.PUBLIC);
		
		var c = LOOKUP.defineClass(def.compile());
		var values = c.getMethod("values").invoke(null);
		assertEquals(0, Array.getLength(values));
	}
	
	@Test
	public void enumConstants() throws Throwable {
		var def = EnumDef.create("fi.benjami.code4jvm.test.EnumConstants", Access.PUBLIC);
		def.addEmptyConstructor(Access.PRIVATE);
		def.addEnumConstant("FOO");
		def.addEnumConstant("BAR");
		
		var c = LOOKUP.defineClass(def.compile());
		
		var a = (Enum<?>) c.getField("FOO").get(null);
		var b = (Enum<?>) c.getField("BAR").get(null);
		assertEquals(0, a.ordinal());
		assertEquals(1, b.ordinal());
		assertEquals("FOO", a.name());
		assertEquals("BAR", b.name());
		
		var values = c.getMethod("values").invoke(null);
		assertEquals(a, Array.get(values, 0));
		assertEquals(b, Array.get(values, 1));
		
		var valueOf = c.getMethod("valueOf", String.class);
		assertEquals(a, valueOf.invoke(null, "FOO"));
		assertEquals(b, valueOf.invoke(null, "BAR"));
	}
	
	@Test
	public void enumConstructor() throws Throwable {
		var def = EnumDef.create("fi.benjami.code4jvm.test.EnumConstructor", Access.PUBLIC);
		
		def.addInstanceField(Access.PUBLIC, Type.INT, "number", FieldFlag.FINAL);
		def.addInstanceField(Access.PUBLIC, Type.STRING, "string", FieldFlag.FINAL);
		
		var constructor = def.addConstructor(Access.PRIVATE);
		var numArg = constructor.arg(Type.INT);
		var strArg = constructor.arg(Type.STRING);
		constructor.add(constructor.self().putField("number", numArg));
		constructor.add(constructor.self().putField("string", strArg));
		constructor.add(Return.nothing());
		
		def.addEnumConstant("FOO", Constant.of(100), Constant.of("foo"));
		def.addEnumConstant("BAR", block -> {
			var result = block.add(Arithmetic.add(Constant.of(10), Constant.of(20))).value();
			return new Value[] {result, Constant.of("bar")};
		});
		
		var c = LOOKUP.defineClass(def.compile());
		
		var a = (Enum<?>) c.getField("FOO").get(null);
		var b = (Enum<?>) c.getField("BAR").get(null);
		assertEquals(0, a.ordinal());
		assertEquals(1, b.ordinal());
		assertEquals("FOO", a.name());
		assertEquals("BAR", b.name());
		
		assertEquals(100, c.getField("number").get(a));
		assertEquals(30, c.getField("number").get(b));
		assertEquals("foo", c.getField("string").get(a));
		assertEquals("bar", c.getField("string").get(b));
		
		var values = c.getMethod("values").invoke(null);
		assertEquals(a, Array.get(values, 0));
		assertEquals(b, Array.get(values, 1));
		
		var valueOf = c.getMethod("valueOf", String.class);
		assertEquals(a, valueOf.invoke(null, "FOO"));
		assertEquals(b, valueOf.invoke(null, "BAR"));
	}

}
