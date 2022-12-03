package fi.benjami.code4jvm.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import fi.benjami.code4jvm.Constant;
import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.Value;
import fi.benjami.code4jvm.config.CompileOptions;
import fi.benjami.code4jvm.config.CoreOptions;
import fi.benjami.code4jvm.flag.Access;
import fi.benjami.code4jvm.statement.Return;
import fi.benjami.code4jvm.statement.StringConcat;
import fi.benjami.code4jvm.typedef.ClassDef;

@ExtendWith({EnableDebugExtension.class})
public class StringConcatTest {
	
	private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

	@Test
	public void noArgsConcat() throws Throwable {
		var def = ClassDef.create("fi.benjami.code4jvm.test.NoArgsConcat", Access.PUBLIC);
		def.addEmptyConstructor(Access.PUBLIC);
		def.interfaces(Type.of(Supplier.class));
		
		var method = def.addMethod(Type.OBJECT, "get", Access.PUBLIC);
		var value = method.add(StringConcat.concat());
		method.add(Return.value(value));
		
		var lookup = LOOKUP.defineHiddenClass(def.compile(), true);
		var instance = (Supplier<?>) TestUtils.newInstance(lookup);
		assertEquals("", instance.get());
	}
	
	@Test
	public void concatArgs() throws Throwable {
		var def = ClassDef.create("fi.benjami.code4jvm.test.ConcatArgs", Access.PUBLIC);
		def.addEmptyConstructor(Access.PUBLIC);
		def.interfaces(Type.of(BiFunction.class));
		
		var method = def.addMethod(Type.OBJECT, "apply", Access.PUBLIC);
		var arg1 = method.arg(Type.OBJECT).cast(Type.STRING);
		var arg2 = method.arg(Type.OBJECT).cast(Type.STRING);
		var value = method.add(StringConcat.concat(arg1, arg2));
		method.add(Return.value(value));
		
		var lookup = LOOKUP.defineHiddenClass(def.compile(), true);
		@SuppressWarnings("unchecked")
		var instance = (BiFunction<String, String, String>) TestUtils.newInstance(lookup);
		assertEquals("foobar", instance.apply("foo", "bar"));
	}
	
	@Test
	public void concatWithConstants() throws Throwable {
		var def = ClassDef.create("fi.benjami.code4jvm.test.ConcatWithConstants", Access.PUBLIC);
		def.addEmptyConstructor(Access.PUBLIC);
		def.interfaces(Type.of(BiFunction.class));
		
		var method = def.addMethod(Type.OBJECT, "apply", Access.PUBLIC);
		var arg1 = method.arg(Type.OBJECT).cast(Type.STRING);
		var arg2 = method.arg(Type.OBJECT).cast(Type.STRING);
		var value = method.add(StringConcat.concat(arg1, Constant.of(false), Constant.of('a'),
				Constant.of(10), Constant.of(4f), Constant.of(3d), arg2, Constant.of("test")));
		method.add(Return.value(value));
		
		var lookup = LOOKUP.defineHiddenClass(def.compile(), true);
		@SuppressWarnings("unchecked")
		var instance = (BiFunction<String, String, String>) TestUtils.newInstance(lookup);
		assertEquals("foofalsea104.03.0bartest", instance.apply("foo", "bar"));
	}
	
	@Test
	public void concatWithConstants2() throws Throwable {
		var def = ClassDef.create("fi.benjami.code4jvm.test.ConcatWithConstants2", Access.PUBLIC);
		def.addEmptyConstructor(Access.PUBLIC);
		def.interfaces(Type.of(BiFunction.class));
		
		var method = def.addMethod(Type.OBJECT, "apply", Access.PUBLIC);
		var arg1 = method.arg(Type.OBJECT).cast(Type.STRING);
		var arg2 = method.arg(Type.OBJECT).cast(Type.STRING);
		var value = method.add(StringConcat.concat(arg1, Constant.of("\1"), arg2, Constant.of("test")));
		method.add(Return.value(value));
		
		var lookup = LOOKUP.defineHiddenClass(def.compile(), true);
		@SuppressWarnings("unchecked")
		var instance = (BiFunction<String, String, String>) TestUtils.newInstance(lookup);
		assertEquals("foo\1bartest", instance.apply("foo", "bar"));
	}
	
	@Test
	public void manyStringsConcat() throws Throwable {
		// This test automatically falls back to StringBuilder due to too many values
		// In theory, StringConcatFactory could support this because the values are
		// constants, but such large concatenations are probably quite rare
		var def = ClassDef.create("fi.benjami.code4jvm.test.ManyStringsConcat", Access.PUBLIC);
		def.addEmptyConstructor(Access.PUBLIC);
		def.interfaces(Type.of(Supplier.class));
		
		var method = def.addMethod(Type.OBJECT, "get", Access.PUBLIC);
		var values = Collections.nCopies(300, Constant.of("foo")).toArray(Value[]::new);
		var value = method.add(StringConcat.concat(values));
		method.add(Return.value(value));
		
		var lookup = LOOKUP.defineHiddenClass(def.compile(), true);
		var instance = (Supplier<?>) TestUtils.newInstance(lookup);
		assertEquals("foo".repeat(300), instance.get());
	}
	
	@Test
	public void stringBuilderCOncat() throws Throwable {
		var def = ClassDef.create("fi.benjami.code4jvm.test.StringBuilderConcat", Access.PUBLIC);
		def.addEmptyConstructor(Access.PUBLIC);
		def.interfaces(Type.of(BiFunction.class));
		
		var method = def.addMethod(Type.OBJECT, "apply", Access.PUBLIC);
		var arg1 = method.arg(Type.OBJECT).cast(Type.STRING);
		var arg2 = method.arg(Type.OBJECT).cast(Type.STRING);
		var value = method.add(StringConcat.concat(arg1, Constant.of(false), Constant.of('a'),
				Constant.of(10), Constant.of(4f), Constant.of(3d), arg2, Constant.of("test")));
		method.add(Return.value(value));
		
		var options = CompileOptions.builder()
				.set(CoreOptions.INDY_STRING_CONCAT, false)
				.build();
		var lookup = LOOKUP.defineHiddenClass(def.compile(options), true);
		@SuppressWarnings("unchecked")
		var instance = (BiFunction<String, String, String>) TestUtils.newInstance(lookup);
		assertEquals("foofalsea104.03.0bartest", instance.apply("foo", "bar"));
	}
}
