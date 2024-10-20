package fi.benjami.code4jvm.lua.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fi.benjami.code4jvm.lua.LuaVm;
import fi.benjami.code4jvm.lua.debug.LinkerTrace;
import fi.benjami.code4jvm.lua.debug.LuaDebugOptions;
import fi.benjami.code4jvm.lua.ir.LuaType;
import fi.benjami.code4jvm.lua.runtime.LuaFunction;

public class LinkerTest {

	private LuaVm vm;
	private LinkerTrace trace;
	
	@BeforeEach
	public void beginTrace() {
		vm = new LuaVm();
		trace = new LinkerTrace();
		LuaDebugOptions.linkerTrace = trace;
	}
	
	private MethodHandle getOnlySpecialization() {
		var specializations = ((LuaType.Function) trace.currentPrototype).specializations();
		assertEquals(1, specializations.size());
		for (var func : specializations.values()) {
			return func.function();
		}
		throw new AssertionError();
	}
	
	@Test
	public void knownTypes() throws Throwable {
		var result = vm.execute("""
				local function f(a, b)
					return a + b
				end
				return f(1.2, 2.1)
				""");
		assertEquals(3.3, result);
		assertEquals(1, trace.metadata.linkageCount);
		assertEquals(MethodType.methodType(double.class, Object.class, double.class, double.class),
				getOnlySpecialization().bindTo(null).type());
	}
	
	@Test
	public void unknownTypes() throws Throwable {
		vm.globals().set("foo", 1d);
		vm.globals().set("bar", 2d);
		var result = vm.execute("""
				local function f(a, b)
					return a + b
				end
				return f(foo, bar)
				""");
		assertEquals(3d, result);
		assertEquals(1, trace.metadata.linkageCount);
		assertEquals(MethodType.methodType(double.class, Object.class, double.class, double.class),
				getOnlySpecialization().bindTo(null).type());
	}
	
	@Test
	public void changingTypes() throws Throwable {
		var func = (LuaFunction) vm.execute("""
				function g(a, b)
					return a
				end
				return function ()
					return g(foo, bar)
				end
				""");
		
		// First call -> specialization based on runtime types
		vm.globals().set("foo", 1d);
		vm.globals().set("bar", 2d);
		func.call();
		assertEquals(1, trace.metadata.linkageCount);
		assertEquals(MethodType.methodType(double.class, Object.class, double.class, double.class),
				getOnlySpecialization().bindTo(null).type());
		
		// First type change doesn't change anything
		vm.globals().set("foo", "fooval");
		vm.globals().set("bar", "barval");
		func.call();
		assertEquals(2, trace.metadata.linkageCount);
		
		// Change types and call function a few more times
		for (var i = 0; i < 10; i++) {
			vm.globals().set("foo", 1d);
			vm.globals().set("bar", 2d);
			func.call();
			
			vm.globals().set("foo", "fooval");
			vm.globals().set("bar", "barval");
			func.call();
		}
		
		// Linker should stop using runtime types if they change too many times
		// After that, a generic implementation is used and there will be no more linkages
		assertEquals(3, trace.metadata.linkageCount);
	}
	
	@Test
	public void constantSite() throws Throwable {
		var func = (LuaFunction) vm.execute("""
				local function g(a, b)
					return a
				end
				return function (a, b)
					return g(a, b)
				end
				""");
		func.call("foo", "bar");
		assertEquals(1, trace.stableTargets);
	}
	
	@Test
	public void unboxedMath() throws Throwable {
		var func = (LuaFunction) vm.execute("""
				local function sum(a, b)
					return a + b
				end
				return function (a, b, c, d)
					return sum(a, b) + sum(c, d)
				end
				""");
		assertEquals(10, func.call(1, 2, 3, 4));
		assertEquals(2, trace.stableTargets);
		assertFalse(trace.metadata.hasUnknownTypes);
	}
	
	@Test
	public void unboxedMath2() throws Throwable {
		var func = (LuaFunction) vm.execute("""
				local function mul(a, b)
					return a * b
				end
				
				local function sum(a, b)
					return mul(a, 3) + b
				end
				return function (a, b, c, d)
					return sum(a, b) + sum(c, d)
				end
				""");
		assertEquals(18, func.call(1, 2, 3, 4));
		assertEquals(3, trace.stableTargets);
		assertFalse(trace.metadata.hasUnknownTypes);
	}
	
	@AfterEach
	public void cleanup() {
		LuaDebugOptions.linkerTrace = null;
	}
}
