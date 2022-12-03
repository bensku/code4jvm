package fi.benjami.code4jvm.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.invoke.MethodHandles;
import java.util.function.BiFunction;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import fi.benjami.code4jvm.Expression;
import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.Value;
import fi.benjami.code4jvm.flag.Access;
import fi.benjami.code4jvm.statement.Arithmetic;
import fi.benjami.code4jvm.statement.Return;
import fi.benjami.code4jvm.typedef.ClassDef;

@ExtendWith({EnableDebugExtension.class})
public class ArithmeticTest {

	private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

	// TODO parameterized test for all number types
	
	private void makeTester(ClassDef def, Type type, String name, BiFunction<Value, Value, Expression> op) {
		var method = def.addMethod(type, name, Access.PUBLIC);
		var a = method.arg(type);
		var b = method.arg(type);
		var sum = method.add(op.apply(a, b));
		method.add(Return.value(sum));
	}
	
	private void makeTesters(ClassDef def, BiFunction<Value, Value, Expression> op) {
		makeTester(def, Type.BYTE, "testByte", op);
		makeTester(def, Type.SHORT, "testShort", op);
		makeTester(def, Type.CHAR, "testChar", op);
		makeTester(def, Type.INT, "testInt", op);
		makeTester(def, Type.LONG, "testLong", op);
		makeTester(def, Type.FLOAT, "testFloat", op);
		makeTester(def, Type.DOUBLE, "testDouble", op);
	}
	
	interface Tester {
		byte testByte(byte a, byte b);
		short testShort(short a, short b);
		char testChar(char a, char b);
		int testInt(int a, int b);
		long testLong(long a, long b);
		float testFloat(float a, float b);
		double testDouble(double a, double b);
	}
	
	@Test
	public void add() throws Throwable {
		var def = ClassDef.create("fi.benjami.code4jvm.test.AddNumbers", Access.PUBLIC);
		def.interfaces(Type.of(Tester.class));
		def.addEmptyConstructor(Access.PUBLIC);
		
		makeTesters(def, Arithmetic::add);
		
		var lookup = LOOKUP.defineHiddenClass(def.compile(), true);
		var instance = (Tester) TestUtils.newInstance(lookup);
		assertEquals(10, instance.testByte((byte) 5, (byte) 5));
		assertEquals(-96, instance.testByte((byte) 80, (byte) 80));
		assertEquals(50, instance.testShort((short) -50, (short) 100));
		assertEquals(84, instance.testChar((char) 42, (char) 42));
		assertEquals(12345, instance.testInt(12300, 45));
		assertEquals(9.1f, instance.testFloat(4.5f, 4.6f));
		assertEquals(123.45, instance.testDouble(123, 0.45));
	}
	
	// TODO explicitly test all data types of all operations
	
	@Test
	public void subtract() throws Throwable {
		var def = ClassDef.create("fi.benjami.code4jvm.test.SubtractNumbers", Access.PUBLIC);
		def.interfaces(Type.of(Tester.class));
		def.addEmptyConstructor(Access.PUBLIC);
		
		makeTesters(def, Arithmetic::subtract);
		
		var lookup = LOOKUP.defineHiddenClass(def.compile(), true);
		var instance = (Tester) TestUtils.newInstance(lookup);
		assertEquals(-5, instance.testInt(5, 10));
	}
	
	@Test
	public void multiply() throws Throwable {
		var def = ClassDef.create("fi.benjami.code4jvm.test.MultiplyNumbers", Access.PUBLIC);
		def.interfaces(Type.of(Tester.class));
		def.addEmptyConstructor(Access.PUBLIC);
		
		makeTesters(def, Arithmetic::multiply);
		
		var lookup = LOOKUP.defineHiddenClass(def.compile(), true);
		var instance = (Tester) TestUtils.newInstance(lookup);
		assertEquals(9, instance.testInt(3, 3));
	}
	
	@Test
	public void divide() throws Throwable {
		var def = ClassDef.create("fi.benjami.code4jvm.test.DivideNumbers", Access.PUBLIC);
		def.interfaces(Type.of(Tester.class));
		def.addEmptyConstructor(Access.PUBLIC);
		
		makeTesters(def, Arithmetic::divide);
		
		var lookup = LOOKUP.defineHiddenClass(def.compile(), true);
		var instance = (Tester) TestUtils.newInstance(lookup);
		assertEquals(24, instance.testInt(120, 5));
	}
	
	@Test
	public void remainder() throws Throwable {
		var def = ClassDef.create("fi.benjami.code4jvm.test.RemainderOfNumbers", Access.PUBLIC);
		def.interfaces(Type.of(Tester.class));
		def.addEmptyConstructor(Access.PUBLIC);
		
		makeTesters(def, Arithmetic::remainder);
		
		var lookup = LOOKUP.defineHiddenClass(def.compile(), true);
		var instance = (Tester) TestUtils.newInstance(lookup);
		assertEquals(4, instance.testInt(10, 6));
	}
}
