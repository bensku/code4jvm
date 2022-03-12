package fi.benjami.code4jvm.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;

import fi.benjami.code4jvm.ClassDef;
import fi.benjami.code4jvm.Constant;
import fi.benjami.code4jvm.Method;
import fi.benjami.code4jvm.Value;
import fi.benjami.code4jvm.flag.Access;
import fi.benjami.code4jvm.statement.Return;

public class ValuesTest {

	private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
	
	public class ValueProvider {
		public String hello() {
			return "hello";
		}
		public int number() {
			return 128;
		}
	}

	@Test
	public void simpleValues() throws Throwable {
		var def = ClassDef.create("fi.benjami.code4jvm.test.SimpleValues", Access.PUBLIC);
		def.addEmptyConstructor(Access.PUBLIC);
		
		var method = def.addMethod(Type.INT_TYPE, "test", Access.PUBLIC);
		// Declare arguments
		var provider = method.arg(Type.getType(ValueProvider.class));
		var consumer = method.arg(Type.getType(Consumer.class));
		
		// Call methods on first argument
		var hello = method.add(provider.virtualLookup(false).call(Type.getType(String.class), "hello")).value();
		var number = method.add(provider.virtualLookup(false).call(Type.INT_TYPE, "number")).value();
		
		// Call the second argument with the string we've got
		method.add(consumer.virtualLookup(true).call(Type.VOID_TYPE, "accept", hello.cast(Type.getType(Object.class))));
		method.add(Return.value(number)); // Return the number we got
				
		// Set up the callback used as second argument
		var holder = new Object() {
			String value;
		};
		Consumer<String> callback = (str) -> holder.value = str;
		
		var lookup = LOOKUP.defineHiddenClass(def.compile(), true);
		int returnValue = (int) lookup.findVirtual(lookup.lookupClass(),
				"test", MethodType.methodType(int.class, ValueProvider.class, Consumer.class))
				.invoke(TestUtils.newInstance(lookup), new ValueProvider(), callback);
		assertEquals(128, returnValue);
		assertEquals("hello", holder.value);
	}
	
	public interface PrimitiveCasts {
		byte int2Byte(int value);
		int byte2Int(byte value);
		int long2Int(long value);
		int float2Int(float value);
		int double2Int(double value);
		long int2Long(int value);
		long float2Long(float value);
		long double2Long(double value);
		float int2Float(int value);
		float long2Float(long value);
		float double2Float(double value);
		double int2Double(int value);
		double long2Double(long value);
		double float2Double(float value);
	}
	
	@Test
	public void primitiveCasts() throws Throwable {
		var def = ClassDef.create("fi.benjami.code4jvm.test.PrimitiveCasts", Access.PUBLIC);
		def.interfaces(Type.getType(PrimitiveCasts.class));
		def.addEmptyConstructor(Access.PUBLIC);
		
		castTester(def, "int2Byte", Type.INT_TYPE, Type.BYTE_TYPE);
		castTester(def, "byte2Int", Type.BYTE_TYPE, Type.INT_TYPE);
		
		castTester(def, "long2Int", Type.LONG_TYPE, Type.INT_TYPE);
		castTester(def, "float2Int", Type.FLOAT_TYPE, Type.INT_TYPE);
		castTester(def, "double2Int", Type.DOUBLE_TYPE, Type.INT_TYPE);
		
		castTester(def, "int2Long", Type.INT_TYPE, Type.LONG_TYPE);
		castTester(def, "float2Long", Type.FLOAT_TYPE, Type.LONG_TYPE);
		castTester(def, "double2Long", Type.DOUBLE_TYPE, Type.LONG_TYPE);
		
		castTester(def, "int2Float", Type.INT_TYPE, Type.FLOAT_TYPE);
		castTester(def, "long2Float", Type.LONG_TYPE, Type.FLOAT_TYPE);
		castTester(def, "double2Float", Type.DOUBLE_TYPE, Type.FLOAT_TYPE);
		
		castTester(def, "int2Double", Type.INT_TYPE, Type.DOUBLE_TYPE);
		castTester(def, "long2Double", Type.LONG_TYPE, Type.DOUBLE_TYPE);
		castTester(def, "float2Double", Type.FLOAT_TYPE, Type.DOUBLE_TYPE);
		
		var lookup = LOOKUP.defineHiddenClass(def.compile(), true);
		// JVM verifier should catch missing casts etc.
		
		// But let's test everything ourself just to be sure
		var instance = (PrimitiveCasts) TestUtils.newInstance(lookup);
		assertEquals(42, instance.int2Byte(42));
		assertEquals(-127, instance.int2Byte(129)); // Wrap around
		
		assertEquals(12345, instance.long2Int(12345));
		assertEquals(Integer.MIN_VALUE, instance.long2Int(0x7fffffffL + 1L));
		assertEquals(3, instance.float2Int(3F));
		assertEquals(10, instance.float2Int(10.8F)); // Truncate, don't round
		assertEquals(3, instance.double2Int(3));
		assertEquals(10, instance.double2Int(10.81)); // Truncate, don't round
		
		assertEquals(0x7fffffffL, instance.int2Long(Integer.MAX_VALUE));
		assertEquals(3, instance.float2Long(3F));
		assertEquals(10, instance.float2Long(10.8F)); // Truncate, don't round
		assertEquals(3, instance.double2Long(3));
		assertEquals(10, instance.double2Long(10.81)); // Truncate, don't round
		
		assertEquals(1024F, instance.int2Float(1024));
		assertEquals(10_000F, instance.long2Float(10_000));
		assertEquals(42.12F, instance.double2Float(42.12), 0.01);
		
		assertEquals(1024, instance.int2Double(1024));
		assertEquals(10_000, instance.long2Double(10_000));
		assertEquals(42.12, instance.float2Double(42.12F), 0.01);
	}
	
	interface ObjectCasts {
		Object string2Object(String value);
		String object2String(Object value);
	}
	
	@Test
	public void objectCasts() throws Throwable {
		var def = ClassDef.create("fi.benjami.code4jvm.test.ObjectCasts", Access.PUBLIC);
		def.interfaces(Type.getType(ObjectCasts.class));
		def.addEmptyConstructor(Access.PUBLIC);
		
		castTester(def, "string2Object", Type.getType(String.class), Type.getType(Object.class));
		castTester(def, "object2String", Type.getType(Object.class), Type.getType(String.class));
		
		var lookup = LOOKUP.defineHiddenClass(def.compile(), true);
		var instance = (ObjectCasts) TestUtils.newInstance(lookup);
		assertEquals("test", instance.string2Object("test"));
		assertEquals("test", instance.object2String("test"));
		assertThrows(ClassCastException.class, () -> instance.object2String(new Object()));
	}
	
	private Method castTester(ClassDef def, String name, Type from, Type to) {
		var method = def.addMethod(to, name, Access.PUBLIC);
		var arg = method.arg(from);
		method.add(Return.value(arg.cast(to)));
		return method;
	}
	
	@Test
	public void uninitialized() throws Throwable {
		var def = ClassDef.create("fi.benjami.code4jvm.test.EmptyValue", Access.PUBLIC);
		def.interfaces(Type.getType(Supplier.class));
		def.addEmptyConstructor(Access.PUBLIC);
		
		var method = def.addMethod(Type.getType(Object.class), "get", Access.PUBLIC);
		var value = method.add(Value.uninitialized(Type.getType(String.class))).variable();
		method.add(value.set(Constant.of("ok")));
		method.add(Return.value(value));
		
		var lookup = LOOKUP.defineHiddenClass(def.compile(), true);
		var instance = (Supplier<?>) TestUtils.newInstance(lookup);
		assertEquals("ok", instance.get());
	}
	
	@Test
	public void uninitializedError() throws Throwable {
		var def = ClassDef.create("fi.benjami.code4jvm.test.EmptyValueError", Access.PUBLIC);
		def.interfaces(Type.getType(Supplier.class));
		def.addEmptyConstructor(Access.PUBLIC);
		
		var method = def.addMethod(Type.getType(Object.class), "get", Access.PUBLIC);
		var value = method.add(Value.uninitialized(Type.getType(String.class))).variable();
		value.set(Constant.of("ok")); // Note the missing method.add(...)!
		method.add(Return.value(value));
		
		assertThrows(IllegalStateException.class, () -> def.compile());
	}
}
