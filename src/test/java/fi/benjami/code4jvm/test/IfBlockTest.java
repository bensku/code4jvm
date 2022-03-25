package fi.benjami.code4jvm.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import fi.benjami.code4jvm.ClassDef;
import fi.benjami.code4jvm.CompileOptions;
import fi.benjami.code4jvm.Condition;
import fi.benjami.code4jvm.Constant;
import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.flag.Access;
import fi.benjami.code4jvm.statement.Return;
import fi.benjami.code4jvm.structure.IfBlock;

public class IfBlockTest {

	private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
	
	@Test
	public void simpleIf() throws Throwable {
		var def = ClassDef.create("fi.benjami.code4jvm.test.SimpleIf", Access.PUBLIC);
		def.addEmptyConstructor(Access.PUBLIC);
		def.interfaces(Type.of(Function.class));
		
		var method = def.addMethod(Type.OBJECT, "apply", Access.PUBLIC);
		var arg = method.arg(Type.OBJECT);
		method.add(IfBlock.with(Condition.equal(arg, Constant.of("ok")))
				.then(block -> {
					block.add(Return.value(Constant.of("success")));
				}));
		method.add(Return.value(Constant.of("fail")));
		
		var lookup = LOOKUP.defineHiddenClass(def.compile(new CompileOptions(true)), true);
		@SuppressWarnings("unchecked")
		var instance = (Function<Object, Object>) TestUtils.newInstance(lookup);
		assertEquals("success", instance.apply("ok"));
		assertEquals("fail", instance.apply("foo"));
		assertEquals("fail", instance.apply(null));
	}
	
	@Test
	public void ifElse() throws Throwable {
		var def = ClassDef.create("fi.benjami.code4jvm.test.IfElse", Access.PUBLIC);
		def.addEmptyConstructor(Access.PUBLIC);
		def.interfaces(Type.of(Function.class));
		
		var method = def.addMethod(Type.OBJECT, "apply", Access.PUBLIC);
		var arg = method.arg(Type.OBJECT);
		method.add(IfBlock.with(Condition.equal(arg, Constant.of("ok")))
				.then(block -> {
					block.add(Return.value(Constant.of("success")));
				}).orElse(block -> {
					block.add(Return.value(Constant.of("else")));
				}));
		method.add(Return.value(Constant.of("fail")));
		Files.write(Path.of("Debug.class"), def.compile());
		
		var lookup = LOOKUP.defineHiddenClass(def.compile(new CompileOptions(true)), true);
		@SuppressWarnings("unchecked")
		var instance = (Function<Object, Object>) TestUtils.newInstance(lookup);
		assertEquals("success", instance.apply("ok"));
		assertEquals("else", instance.apply("foo"));
		assertEquals("else", instance.apply(null));
	}
	
	@Test
	public void complexIf() throws Throwable {
		var def = ClassDef.create("fi.benjami.code4jvm.test.ComplexIf", Access.PUBLIC);
		def.addEmptyConstructor(Access.PUBLIC);
		def.interfaces(Type.of(Function.class));
		
		var method = def.addMethod(Type.OBJECT, "apply", Access.PUBLIC);
		var arg = method.arg(Type.OBJECT);
		method.add(IfBlock.with(Condition.equal(arg, Constant.of("ok")))
				.then(block -> {
					block.add(Return.value(Constant.of("success")));
				}).elseIf(Condition.equal(arg, Constant.of("alt")), block -> {
					block.add(Return.value(Constant.of("elif")));
				}).orElse(block -> {
					block.add(Return.value(Constant.of("else")));
				}));
		method.add(Return.value(Constant.of("fail")));
		
		var lookup = LOOKUP.defineHiddenClass(def.compile(new CompileOptions(true)), true);
		@SuppressWarnings("unchecked")
		var instance = (Function<Object, Object>) TestUtils.newInstance(lookup);
		assertEquals("success", instance.apply("ok"));
		assertEquals("elif", instance.apply("alt"));
		assertEquals("else", instance.apply("foo"));
		assertEquals("else", instance.apply(null));
	}
}
