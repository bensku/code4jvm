package fi.benjami.code4jvm.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;

import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.block.Lambda;
import fi.benjami.code4jvm.config.CompileOptions;
import fi.benjami.code4jvm.flag.Access;
import fi.benjami.code4jvm.statement.Return;
import fi.benjami.code4jvm.typedef.ClassDef;

@ExtendWith({EnableDebugExtension.class})
public class LambdaTest {

	private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

	private Lambda returnArg() {
		var lambda = Lambda.create(Type.OBJECT);
		var arg = lambda.arg(Type.OBJECT);
		lambda.add(Return.value(arg));
		return lambda;
	}
	
	@ParameterizedTest
	@OptionsSource
	public void staticMethod(CompileOptions opts) throws Throwable {
		var def = ClassDef.create("fi.benjami.code4jvm.test.LambdaStaticMethod", Access.PUBLIC);
		def.addEmptyConstructor(Access.PUBLIC);
		
		var method = returnArg().addStaticMethod(def);
		
		var lookup = TestUtils.loadHidden(def, opts);
		assertEquals("ok", lookup.findStatic(lookup.lookupClass(), method.name(), MethodType.methodType(Object.class, Object.class))
				.invoke("ok"));
	}
	
	@Test
	public void instanceMethod() throws Throwable {
		// TODO parameterize once duplicate class problem has been figured out
		var def = ClassDef.create("fi.benjami.code4jvm.test.LambdaInstanceMethod", Access.PUBLIC);
		def.addEmptyConstructor(Access.PUBLIC);
		
		var lambda = Lambda.create(def.type());
		var arg = lambda.arg(def.type());
		lambda.add(Return.value(arg));
		var method = lambda.addInstanceMethod(def);
		
		var c = LOOKUP.defineClass(def.compile());
		var instance = c.getConstructor().newInstance();
		assertEquals(instance, LOOKUP.findVirtual(c, method.name(), MethodType.methodType(c))
				.invoke(instance));
	}
	
	@ParameterizedTest
	@OptionsSource
	public void directCall(CompileOptions opts) throws Throwable {
		var def = ClassDef.create("fi.benjami.code4jvm.test.LambdaDirectCall", Access.PUBLIC);
		def.addEmptyConstructor(Access.PUBLIC);
				
		var redirect = Lambda.create(Type.OBJECT);
		var arg = redirect.arg(Type.OBJECT);
		var value = redirect.add(returnArg().call(arg));
		redirect.add(Return.value(value));
		var method = redirect.addStaticMethod(def);
		
		var lookup = TestUtils.loadHidden(def, opts);
		assertEquals("ok", lookup.findStatic(lookup.lookupClass(), method.name(), MethodType.methodType(Object.class, Object.class))
				.invoke("ok"));
	}
	
	@Test
	public void lambdaInstance() throws Throwable {
		// TODO parameterize once duplicate class problem has been figured out
		var def = ClassDef.create("fi.benjami.code4jvm.test.LambdaNewInstance", Access.PUBLIC);
		def.addEmptyConstructor(Access.PUBLIC);
		
		var lambda = Lambda.create(Type.of(Function.class));
		var instance = lambda.add(returnArg().newInstance(Lambda.shape(Type.of(Function.class), "apply", Type.OBJECT, Type.OBJECT)));
		lambda.add(Return.value(instance));
		var method = lambda.addStaticMethod(def);
		
		Class<?> c = LOOKUP.defineClass(def.compile());
		var func = (Function<Object, Object>) LOOKUP.findStatic(c, method.name(), MethodType.methodType(Function.class))
				.invoke();
		assertEquals("ok", func.apply("ok"));
	}
	
	public class CallMe {
		public String callMe(String param) {
			if (!"foo".equals(param)) {
				throw new IllegalArgumentException();
			}
			return "bar";
		}
	}
	
	@Test
	public void variableCapture() throws Throwable {
		// TODO parameterize once duplicate class problem has been figured out
		var def = ClassDef.create("fi.benjami.code4jvm.test.LambdaVariableCapture", Access.PUBLIC);
		def.addEmptyConstructor(Access.PUBLIC);
		
		var target = Lambda.create(Type.of(Object.class));
		var arg1 = target.arg(Type.of(CallMe.class));
		var arg2 = target.arg(Type.of(Object.class)).cast(Type.STRING);
		var result = target.add(arg1.callVirtual(Type.STRING, "callMe", arg2));
		target.add(Return.value(result.cast(Type.OBJECT)));
		
		var lambda = Lambda.create(Type.of(Function.class));
		var callMe = lambda.arg(Type.of(CallMe.class));
		var instance = lambda.add(target.newInstance(Type.of(Function.class), "apply", callMe));
		lambda.add(Return.value(instance));
		var method = lambda.addStaticMethod(def);
		
		Class<?> c = LOOKUP.defineClass(def.compile());
		var func = (Function<Object, Object>) LOOKUP.findStatic(c, method.name(), MethodType.methodType(Function.class, CallMe.class))
				.invoke(new CallMe());
		assertEquals("bar", func.apply("foo"));
	}
}
