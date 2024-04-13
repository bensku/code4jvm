package fi.benjami.code4jvm.lua.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
				return f(1, 2)
				""");
		assertEquals(3d, result);
		assertEquals(1, trace.metadata.linkageCount);
		assertEquals(0, trace.metadata.typeChangeCount);
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
		assertEquals(1, trace.metadata.typeChangeCount);
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
		assertEquals(1, trace.metadata.typeChangeCount);
		assertEquals(MethodType.methodType(double.class, Object.class, double.class, double.class),
				getOnlySpecialization().bindTo(null).type());
		
		// First type change doesn't change anything
		vm.globals().set("foo", "fooval");
		vm.globals().set("bar", "barval");
		func.call();
		assertEquals(2, trace.metadata.linkageCount);
		assertEquals(2, trace.metadata.typeChangeCount);
		
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
		assertEquals(4, trace.metadata.linkageCount);
		assertEquals(3, trace.metadata.typeChangeCount); // Last linkage does not use runtime types
	}
	
	@AfterEach
	public void cleanup() {
		LuaDebugOptions.linkerTrace = null;
	}
}
