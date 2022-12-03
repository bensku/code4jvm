package fi.benjami.code4jvm.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import fi.benjami.code4jvm.Condition;
import fi.benjami.code4jvm.Constant;
import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.UninitializedValueException;
import fi.benjami.code4jvm.Variable;
import fi.benjami.code4jvm.block.Block;
import fi.benjami.code4jvm.block.Method;
import fi.benjami.code4jvm.flag.Access;
import fi.benjami.code4jvm.statement.Arithmetic;
import fi.benjami.code4jvm.statement.Jump;
import fi.benjami.code4jvm.statement.Return;
import fi.benjami.code4jvm.typedef.ClassDef;

@ExtendWith({EnableDebugExtension.class})
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
		
		var method = def.addMethod(Type.INT, "test", Access.PUBLIC);
		// Declare arguments
		var provider = method.arg(Type.of(ValueProvider.class));
		var consumer = method.arg(Type.of(Consumer.class));
		
		// Call methods on first argument
		var hello = method.add(provider.callVirtual(Type.STRING, "hello"));
		var number = method.add(provider.callVirtual(Type.INT, "number"));
		
		// Call the second argument with the string we've got
		method.add(consumer.callVirtual(Type.VOID, "accept", hello.cast(Type.of(Object.class))));
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
		def.interfaces(Type.of(PrimitiveCasts.class));
		def.addEmptyConstructor(Access.PUBLIC);
		
		castTester(def, "int2Byte", Type.INT, Type.BYTE);
		castTester(def, "byte2Int", Type.BYTE, Type.INT);
		
		castTester(def, "long2Int", Type.LONG, Type.INT);
		castTester(def, "float2Int", Type.FLOAT, Type.INT);
		castTester(def, "double2Int", Type.DOUBLE, Type.INT);
		
		castTester(def, "int2Long", Type.INT, Type.LONG);
		castTester(def, "float2Long", Type.FLOAT, Type.LONG);
		castTester(def, "double2Long", Type.DOUBLE, Type.LONG);
		
		castTester(def, "int2Float", Type.INT, Type.FLOAT);
		castTester(def, "long2Float", Type.LONG, Type.FLOAT);
		castTester(def, "double2Float", Type.DOUBLE, Type.FLOAT);
		
		castTester(def, "int2Double", Type.INT, Type.DOUBLE);
		castTester(def, "long2Double", Type.LONG, Type.DOUBLE);
		castTester(def, "float2Double", Type.FLOAT, Type.DOUBLE);
		
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
		def.interfaces(Type.of(ObjectCasts.class));
		def.addEmptyConstructor(Access.PUBLIC);
		
		castTester(def, "string2Object", Type.STRING, Type.of(Object.class));
		castTester(def, "object2String", Type.of(Object.class), Type.STRING);
		
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
	public void unboundValue() throws Throwable {
		var def = ClassDef.create("fi.benjami.code4jvm.test.UnboundValue", Access.PUBLIC);
		def.interfaces(Type.of(Supplier.class));
		def.addEmptyConstructor(Access.PUBLIC);
		
		var method = def.addMethod(Type.OBJECT, "get", Access.PUBLIC);
		var value = Variable.create(Type.STRING);
		method.add(value.set(Constant.of("ok")));
		method.add(Return.value(value));
		
		var lookup = LOOKUP.defineHiddenClass(def.compile(), true);
		var instance = (Supplier<?>) TestUtils.newInstance(lookup);
		assertEquals("ok", instance.get());
	}
	
	@Test
	public void unboundValueError() throws Throwable {
		var def = ClassDef.create("fi.benjami.code4jvm.test.UnboundValueError", Access.PUBLIC);
		def.interfaces(Type.of(Supplier.class));
		def.addEmptyConstructor(Access.PUBLIC);
		
		var method = def.addMethod(Type.OBJECT, "get", Access.PUBLIC);
		var value = Variable.create(Type.STRING);
		value.set(Constant.of("ok")); // Note the missing method.add(...)!
		method.add(Return.value(value));
		
		assertThrows(UninitializedValueException.class, () -> def.compile());
	}
	
	@Test
	public void useBeforeDefinition() {
		var def = ClassDef.create("fi.benjami.code4jvm.test.UseBeforeDefinition", Access.PUBLIC);
		def.interfaces(Type.of(Supplier.class));
		def.addEmptyConstructor(Access.PUBLIC);
		
		var method = def.addMethod(Type.OBJECT, "get", Access.PUBLIC);
		var block = Block.create();
		var value = block.add(Arithmetic.add(Constant.of(1), Constant.of(2)));
		method.add(Arithmetic.add(value, Constant.of(3)));
		method.add(block);
		method.add(Return.value(Constant.of("ok")));
		
		assertThrows(UninitializedValueException.class, () -> def.compile());
	}
	
	@Test
	public void maybeUninitialized() {
		var def = ClassDef.create("fi.benjami.code4jvm.test.MaybeUninitialized", Access.PUBLIC);
		def.interfaces(Type.of(Supplier.class));
		def.addEmptyConstructor(Access.PUBLIC);
		
		var method = def.addMethod(Type.OBJECT, "get", Access.PUBLIC);
		var a = Block.create();
		var value = a.add(Arithmetic.add(Constant.of(1), Constant.of(2)));
		var b = Block.create();
		method.add(Jump.to(b, Jump.Target.START, Condition.isTrue(Constant.of(true))));
		method.add(a);
		method.add(b);
		method.add(Arithmetic.add(value, Constant.of(3)));
		method.add(Return.value(Constant.of("ok")));
		
		assertThrows(UninitializedValueException.class, () -> def.compile());
	}
	
	@Test
	public void charConstant() throws Throwable {
		var def = ClassDef.create("fi.benjami.code4jvm.test.NonIntConstant", Access.PUBLIC);
		def.addEmptyConstructor(Access.PUBLIC);
		
		var method = def.addMethod(Type.CHAR, "get", Access.PUBLIC);
		method.add(Return.value(Constant.of('x')));
		
		var lookup = LOOKUP.defineHiddenClass(def.compile(), true);
		var instance = TestUtils.newInstance(lookup);
		assertEquals('x', (char) lookup.findVirtual(lookup.lookupClass(), "get", MethodType.methodType(char.class))
				.invoke(instance));
	}
	
	@Test
	public void shortConstant() throws Throwable {
		var def = ClassDef.create("fi.benjami.code4jvm.test.ShortConstant", Access.PUBLIC);
		def.addEmptyConstructor(Access.PUBLIC);
		
		var method = def.addMethod(Type.SHORT, "get", Access.PUBLIC);
		method.add(Return.value(Constant.of((short) -5)));
		
		var lookup = LOOKUP.defineHiddenClass(def.compile(), true);
		var instance = TestUtils.newInstance(lookup);
		assertEquals((short) -5, (short) lookup.findVirtual(lookup.lookupClass(), "get", MethodType.methodType(short.class))
				.invoke(instance));
	}
}
