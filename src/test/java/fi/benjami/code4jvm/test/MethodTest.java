package fi.benjami.code4jvm.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;
import java.util.function.Supplier;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;

import fi.benjami.code4jvm.Constant;
import fi.benjami.code4jvm.InvalidReturnTypeException;
import fi.benjami.code4jvm.MissingReturnException;
import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.call.CallTarget;
import fi.benjami.code4jvm.config.CompileOptions;
import fi.benjami.code4jvm.flag.Access;
import fi.benjami.code4jvm.statement.Return;
import fi.benjami.code4jvm.typedef.ClassDef;

@ExtendWith({EnableDebugExtension.class})
public class MethodTest {
	
	@ParameterizedTest
	@OptionsSource
	public void empty(CompileOptions opts) throws Throwable {
		var def = ClassDef.create("fi.benjami.code4jvm.test.EmptyMethod", Access.PUBLIC);
		def.addEmptyConstructor(Access.PUBLIC);
		
		var method = def.addMethod(Type.VOID, "doNothing", Access.PUBLIC);
		method.add(Return.nothing());
		
		var lookup = TestUtils.loadHidden(def, opts);
		lookup.findVirtual(lookup.lookupClass(), "doNothing", MethodType.methodType(void.class))
				.invoke(TestUtils.newInstance(lookup));
	}
	
	@ParameterizedTest
	@OptionsSource
	public void returning(CompileOptions opts) throws Throwable {
		var def = ClassDef.create("fi.benjami.code4jvm.test.ReturningMethod", Access.PUBLIC);
		def.addEmptyConstructor(Access.PUBLIC);
		
		def.addMethod(Type.INT, "returnInt", Access.PUBLIC).add(Return.value(Constant.of(1337)));
		def.addMethod(Type.LONG, "returnLong", Access.PUBLIC).add(Return.value(Constant.of(42L)));
		def.addMethod(Type.STRING, "returnString", Access.PUBLIC).add(Return.value(Constant.of("hello")));
		
		var lookup = TestUtils.loadHidden(def, opts);
		var instance = TestUtils.newInstance(lookup);
		assertEquals(1337, (int) lookup.findVirtual(lookup.lookupClass(), "returnInt", MethodType.methodType(int.class))
				.invoke(instance));
		assertEquals(42, (long) lookup.findVirtual(lookup.lookupClass(), "returnLong", MethodType.methodType(long.class))
				.invoke(instance));
		assertEquals("hello", (String) lookup.findVirtual(lookup.lookupClass(), "returnString", MethodType.methodType(String.class))
				.invoke(instance));
	}
	
	@ParameterizedTest
	@OptionsSource
	public void withArguments(CompileOptions opts) throws Throwable {
		var def = ClassDef.create("fi.benjami.code4jvm.test.WithArguments", Access.PUBLIC);
		def.addEmptyConstructor(Access.PUBLIC);
		
		var method = def.addMethod(Type.of(Object.class), "returnArg", Access.PUBLIC);
		var arg = method.arg(Type.of(Object.class));
		method.add(Return.value(arg));
		
		var lookup = TestUtils.loadHidden(def, opts);
		var obj = new Object();
		assertEquals(obj, lookup.findVirtual(lookup.lookupClass(),
				"returnArg", MethodType.methodType(Object.class, Object.class))
				.invoke(TestUtils.newInstance(lookup), obj));
	}
	
	public record Constructable(
			String value
	) {}
	
	@ParameterizedTest
	@OptionsSource
	public void constructor(CompileOptions opts) throws Throwable {
		var def = ClassDef.create("fi.benjami.code4jvm.test.WithArguments", Access.PUBLIC);
		def.interfaces(Type.of(Supplier.class));
		def.addEmptyConstructor(Access.PUBLIC);
		
		var method = def.addMethod(Type.OBJECT, "get", Access.PUBLIC);
		var obj = method.add(Type.of(Constructable.class).newInstance(Constant.of("ok")));
		method.add(Return.value(obj));
		
		var instance = (Supplier<?>) TestUtils.newInstance(def, opts);
		assertEquals(new Constructable("ok"), instance.get());
	}
	
	private static CallSite CALL_SITE;
	
	@SuppressWarnings("unused") // invokedynamic
	private static boolean targetMethod1(String arg) {
		return arg.equals("ok");
	}
	
	@SuppressWarnings("unused") // invokedynamic
	private static boolean targetMethod2(String arg) {
		return false;
	}
	
	public static CallSite bootstrap(MethodHandles.Lookup lookup, String name, MethodType type, String arg) throws NoSuchMethodException, IllegalAccessException {
		if (!name.equals("methodName") || !arg.equals("extraArg")) {
			throw new AssertionError();
		}
		var target = MethodHandles.lookup().findStatic(MethodTest.class, "targetMethod1",
				MethodType.methodType(boolean.class, String.class));
		CALL_SITE = new MutableCallSite(target);
		return CALL_SITE;
	}
	
	interface BooleanFunction {
		boolean apply(String arg);
	}
	
	@ParameterizedTest
	@OptionsSource
	public void invokeDynamic(CompileOptions opts) throws Throwable {
		var def = ClassDef.create("fi.benjami.code4jvm.test.InvokeDynamic", Access.PUBLIC);
		def.interfaces(Type.of(BooleanFunction.class));
		def.addEmptyConstructor(Access.PUBLIC);
		
		var method = def.addMethod(Type.BOOLEAN, "apply", Access.PUBLIC);
		var arg = method.arg(Type.STRING);
		var bootstrap = Type.of(getClass()).staticMethod(Type.of(CallSite.class), "bootstrap",
				Type.of(MethodHandles.Lookup.class), Type.STRING, Type.of(MethodType.class), Type.STRING);
		CallTarget.dynamic(bootstrap.withCapturedArgs(Constant.of("extraArg")), Type.BOOLEAN, "methodName", Type.STRING);
		var result = method.add(CallTarget.dynamic(bootstrap.withCapturedArgs(Constant.of("extraArg")),
				Type.BOOLEAN, "methodName", Type.STRING)
				.call(arg));
		method.add(Return.value(result));
		
		var instance = (BooleanFunction) TestUtils.newInstance(def, opts);
		assertTrue(instance.apply("ok"));
		assertFalse(instance.apply("something else"));
		
		// Change target and try again
		CALL_SITE.setTarget(MethodHandles.lookup().findStatic(MethodTest.class, "targetMethod2",
				MethodType.methodType(boolean.class, String.class)));
		assertFalse(instance.apply("ok"));
	}
	
	@ParameterizedTest
	@OptionsSource
	public void missingReturn(CompileOptions opts) throws Throwable {
		var def = ClassDef.create("fi.benjami.code4jvm.test.MissingReturn", Access.PUBLIC);
		def.addEmptyConstructor(Access.PUBLIC);
		
		def.addMethod(Type.VOID, "doNothing", Access.PUBLIC);
		
		assertThrows(MissingReturnException.class, () -> def.compile(opts));
	}
	
	@ParameterizedTest
	@OptionsSource
	public void wrongReturnType(CompileOptions opts) throws Throwable {
		var def = ClassDef.create("fi.benjami.code4jvm.test.WrongReturnType", Access.PUBLIC);
		def.addEmptyConstructor(Access.PUBLIC);
		
		var method = def.addMethod(Type.SHORT, "fail1", Access.PUBLIC);
		method.add(Return.nothing());
		
		assertThrows(InvalidReturnTypeException.class, () -> def.compile(opts));
	}
}
