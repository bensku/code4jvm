package fi.benjami.code4jvm.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.function.Function;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;

import fi.benjami.code4jvm.Condition;
import fi.benjami.code4jvm.Constant;
import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.Variable;
import fi.benjami.code4jvm.config.CompileOptions;
import fi.benjami.code4jvm.flag.Access;
import fi.benjami.code4jvm.statement.Return;
import fi.benjami.code4jvm.structure.IfBlock;
import fi.benjami.code4jvm.typedef.ClassDef;

@ExtendWith({EnableDebugExtension.class})
public class IfBlockTest {
	
	@ParameterizedTest
	@OptionsSource
	public void simpleIf(CompileOptions opts) throws Throwable {
		var def = ClassDef.create("fi.benjami.code4jvm.test.SimpleIf", Access.PUBLIC);
		def.addEmptyConstructor(Access.PUBLIC);
		def.interfaces(Type.of(Function.class));
		
		var method = def.addMethod(Type.OBJECT, "apply", Access.PUBLIC);
		var arg = method.arg(Type.OBJECT);
		method.add(new IfBlock()
				.branch(Condition.equal(arg, Constant.of("ok")), block -> {
					block.add(Return.value(Constant.of("success")));
				}));
		method.add(Return.value(Constant.of("fail")));
		
		@SuppressWarnings("unchecked")
		var instance = (Function<Object, Object>) TestUtils.newInstance(def, opts);
		assertEquals("success", instance.apply("ok"));
		assertEquals("fail", instance.apply("foo"));
		assertEquals("fail", instance.apply(null));
	}
	
	@ParameterizedTest
	@OptionsSource
	public void ifElse(CompileOptions opts) throws Throwable {
		var def = ClassDef.create("fi.benjami.code4jvm.test.IfElse", Access.PUBLIC);
		def.addEmptyConstructor(Access.PUBLIC);
		def.interfaces(Type.of(Function.class));
		
		var method = def.addMethod(Type.OBJECT, "apply", Access.PUBLIC);
		var arg = method.arg(Type.OBJECT);
		method.add(new IfBlock()
				.branch(Condition.equal(arg, Constant.of("ok")), block -> {
					block.add(Return.value(Constant.of("success")));
				}).fallback(block -> {
					block.add(Return.value(Constant.of("else")));
				}));
		method.add(Return.value(Constant.of("fail")));
		
		@SuppressWarnings("unchecked")
		var instance = (Function<Object, Object>) TestUtils.newInstance(def, opts);
		assertEquals("success", instance.apply("ok"));
		assertEquals("else", instance.apply("foo"));
		assertEquals("else", instance.apply(null));
	}
	
	@ParameterizedTest
	@OptionsSource
	public void complexIf(CompileOptions opts) throws Throwable {
		var def = ClassDef.create("fi.benjami.code4jvm.test.ComplexIf", Access.PUBLIC);
		def.addEmptyConstructor(Access.PUBLIC);
		def.interfaces(Type.of(Function.class));
		
		var method = def.addMethod(Type.OBJECT, "apply", Access.PUBLIC);
		var arg = method.arg(Type.OBJECT);
		method.add(new IfBlock()
				.branch(Condition.equal(arg, Constant.of("ok")), block -> {
					block.add(Return.value(Constant.of("success")));
				}).branch(Condition.equal(arg, Constant.of("alt")), block -> {
					block.add(Return.value(Constant.of("elif")));
				}).fallback(block -> {
					block.add(Return.value(Constant.of("else")));
				}));
		method.add(Return.value(Constant.of("fail")));
		
		@SuppressWarnings("unchecked")
		var instance = (Function<Object, Object>) TestUtils.newInstance(def, opts);
		assertEquals("success", instance.apply("ok"));
		assertEquals("elif", instance.apply("alt"));
		assertEquals("else", instance.apply("foo"));
		assertEquals("else", instance.apply(null));
	}
	
	@ParameterizedTest
	@OptionsSource
	public void onlyElse(CompileOptions opts) throws Throwable {
		var def = ClassDef.create("fi.benjami.code4jvm.test.OnlyElse", Access.PUBLIC);
		def.addEmptyConstructor(Access.PUBLIC);
		def.interfaces(Type.of(Function.class));
		
		var method = def.addMethod(Type.OBJECT, "apply", Access.PUBLIC);
		method.arg(Type.OBJECT);
		method.add(new IfBlock()
				.fallback(block -> {
					block.add(Return.value(Constant.of("success")));
				}));
		method.add(Return.value(Constant.of("fail")));
		
		@SuppressWarnings("unchecked")
		var instance = (Function<Object, Object>) TestUtils.newInstance(def, opts);
		assertEquals("success", instance.apply("ok"));
		assertEquals("success", instance.apply("foo"));
		assertEquals("success", instance.apply(null));
	}
	
	@ParameterizedTest
	@OptionsSource
	public void testCode(CompileOptions opts) throws Throwable {
		var def = ClassDef.create("fi.benjami.code4jvm.test.TestCode", Access.PUBLIC);
		def.addEmptyConstructor(Access.PUBLIC);
		def.interfaces(Type.of(Function.class));
		
		var method = def.addMethod(Type.OBJECT, "apply", Access.PUBLIC);
		var arg = method.arg(Type.OBJECT);
		method.add(new IfBlock()
				.branch(block -> {
					// argument -> local variable, just to see that test block is properly emitted
					var local = Variable.create(Type.STRING);
					block.add(local.set(arg.cast(Type.STRING)));
					return Condition.equal(local, Constant.of("ok"));
				}, block -> {
					block.add(Return.value(Constant.of("success")));
				}));
		method.add(Return.value(Constant.of("fail")));
		
		@SuppressWarnings("unchecked")
		var instance = (Function<Object, Object>) TestUtils.newInstance(def, opts);
		assertEquals("success", instance.apply("ok"));
		assertEquals("fail", instance.apply("foo"));
		assertEquals("fail", instance.apply(null));
	}
}
