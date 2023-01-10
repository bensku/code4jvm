package fi.benjami.code4jvm.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;

import fi.benjami.code4jvm.Constant;
import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.Value;
import fi.benjami.code4jvm.config.CompileOptions;
import fi.benjami.code4jvm.flag.Access;
import fi.benjami.code4jvm.statement.ArrayAccess;
import fi.benjami.code4jvm.statement.Return;
import fi.benjami.code4jvm.typedef.ClassDef;

@ExtendWith({EnableDebugExtension.class})
public class ArrayTest {

	private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
	
	interface ArrayCreator {
		boolean[] booleans();
		byte[] bytes();
		short[] shorts();
		char[] chars();
		int[] ints();
		long[] longs();
		float[] floats();
		double[] doubles();
		Object[] objects();
		
		int[][] multiInts();
		Object[][][] multiObjects();
	}
	
	private void addCreator(ClassDef def, String name, Type type, int... lengths) {
		var method = def.addMethod(type, name, Access.PUBLIC);
		var constants = Arrays.stream(lengths).mapToObj(len -> Constant.of(len)).toArray(Value[]::new);
		var instance = method.add(type.newInstance(constants));
		method.add(Return.value(instance));
	}
	
	@ParameterizedTest
	@OptionsSource
	public void createArrays(CompileOptions opts) throws Throwable {
		var def = ClassDef.create("fi.benjami.code4jvm.test.CreateArrays", Access.PUBLIC);
		def.interfaces(Type.of(ArrayCreator.class));
		def.addEmptyConstructor(Access.PUBLIC);
		
		addCreator(def, "booleans", Type.BOOLEAN.array(1), 20);
		addCreator(def, "bytes", Type.BYTE.array(1), 20);
		addCreator(def, "shorts", Type.SHORT.array(1), 20);
		addCreator(def, "chars", Type.CHAR.array(1), 20);
		addCreator(def, "ints", Type.INT.array(1), 20);
		addCreator(def, "longs", Type.LONG.array(1), 20);
		addCreator(def, "floats", Type.FLOAT.array(1), 20);
		addCreator(def, "doubles", Type.DOUBLE.array(1), 20);
		addCreator(def, "objects", Type.OBJECT.array(1), 20);
		addCreator(def, "multiInts", Type.INT.array(2), 20, 10);
		addCreator(def, "multiObjects", Type.OBJECT.array(3), 20, 10, 5);
		
		var instance = (ArrayCreator) TestUtils.newInstance(def, opts);
		assertEquals(20, instance.booleans().length);
		assertEquals(20, instance.bytes().length);
		assertEquals(20, instance.shorts().length);
		assertEquals(20, instance.chars().length);
		assertEquals(20, instance.ints().length);
		assertEquals(20, instance.longs().length);
		assertEquals(20, instance.floats().length);
		assertEquals(20, instance.doubles().length);
		assertEquals(20, instance.objects().length);
		
		var multiInts = instance.multiInts();
		assertEquals(20, multiInts.length);
		assertEquals(10, multiInts[19].length);
		var multiObjects = instance.multiObjects();
		assertEquals(20, multiObjects.length);
		assertEquals(10, multiObjects[0].length);
		assertEquals(5, multiObjects[19][9].length);
	}
	
	interface ArrayGetter {
		boolean get(boolean[] array, int index);
		byte get(byte[] array, int index);
		short get(short[] array, int index);
		char get(char[] array, int index);
		int get(int[] array, int index);
		long get(long[] array, int index);
		float get(float[] array, int index);
		double get(double[] array, int index);
		Object get(Object[] array, int index);
		
		int[] get(int[][] array, int index);
		Object[][] get(Object[][][] array, int index);
	}
	
	private void addGetter(ClassDef def, Type type) {
		var method = def.addMethod(type.componentType(1), "get", Access.PUBLIC);
		var array = method.arg(type);
		var index = method.arg(Type.INT);
		var value = method.add(ArrayAccess.get(array, index));
		method.add(Return.value(value));
	}
	
	private void testNumberGet(ArrayGetter getter, Class<?> type) throws Throwable {
		var array = Array.newInstance(type, 256);
		var values = new int[256]; // Separate array for testing
		var rng = new Random(1);
		for (int i = 0; i < 256; i++) {
			var value = (byte) rng.nextInt();
			values[i] = value;
			Array.set(array, i, value);
		}
		
		var getMethod = LOOKUP.findVirtual(ArrayGetter.class, "get",
				MethodType.methodType(type, array.getClass(), int.class));
		for (int i = 0; i < 256; i++) {
			assertEquals(values[i], ((Number) getMethod.invoke(getter, array, i)).intValue());
		}
	}
	
	@ParameterizedTest
	@OptionsSource
	public void getArrays(CompileOptions opts) throws Throwable {
		var def = ClassDef.create("fi.benjami.code4jvm.test.GetArrays", Access.PUBLIC);
		def.interfaces(Type.of(ArrayGetter.class));
		def.addEmptyConstructor(Access.PUBLIC);
		
		addGetter(def, Type.BOOLEAN.array(1));
		addGetter(def, Type.BYTE.array(1));
		addGetter(def, Type.SHORT.array(1));
		addGetter(def,  Type.CHAR.array(1));
		addGetter(def, Type.INT.array(1));
		addGetter(def, Type.LONG.array(1));
		addGetter(def, Type.FLOAT.array(1));
		addGetter(def, Type.DOUBLE.array(1));
		addGetter(def, Type.OBJECT.array(1));
		addGetter(def, Type.INT.array(2));
		addGetter(def, Type.OBJECT.array(3));
		
		var instance = (ArrayGetter) TestUtils.newInstance(def, opts);
		
		var booleans = new boolean[20];
		booleans[19] = true;
		assertFalse(instance.get(booleans, 0));
		assertTrue(instance.get(booleans, 19));
		
		testNumberGet(instance, byte.class);
		testNumberGet(instance, short.class);
		// char is not fully number in Java
		testNumberGet(instance, int.class);
		testNumberGet(instance, long.class);
		testNumberGet(instance, float.class);
		testNumberGet(instance, double.class);
	}
	
	interface ArraySetter {
		void set(boolean[] array, int index, boolean value);
		void set(byte[] array, int index, byte value);
		void set(short[] array, int index, short value);
		void set(char[] array, int index, char value);
		void set(int[] array, int index, int value);
		void set(long[] array, int index, long value);
		void set(float[] array, int index, float value);
		void set(double[] array, int index, double value);
		void set(Object[] array, int index, Object value);
		
		void set(int[][] array, int index, int[] value);
		void set(Object[][][] array, int index, Object[][] value);
	}
	
	private void addSetter(ClassDef def, Type type) {
		var method = def.addMethod(Type.VOID, "set", Access.PUBLIC);
		var array = method.arg(type);
		var index = method.arg(Type.INT);
		var newValue = method.arg(type.componentType(1));
		method.add(ArrayAccess.set(array, index, newValue));
		method.add(Return.nothing());
	}
	
	private void testNumberSet(ArraySetter setter, Class<?> type) throws Throwable {
		var array = Array.newInstance(type, 256);
		var values = new int[256]; // Separate array for testing
		var rng = new Random(1);
		
		var setMethod = LOOKUP.findVirtual(ArraySetter.class, "set",
				MethodType.methodType(void.class, array.getClass(), int.class, type));
		for (int i = 0; i < 256; i++) {
			var value = (byte) rng.nextInt();
			values[i] = value;
			setMethod.invoke(setter, array, i, value);
		}
		
		for (int i = 0; i < 256; i++) {
			assertEquals(values[i], ((Number) Array.get(array, i)).intValue());
		}
	}
	
	@ParameterizedTest
	@OptionsSource
	public void setArrays(CompileOptions opts) throws Throwable {
		var def = ClassDef.create("fi.benjami.code4jvm.test.SetArrays", Access.PUBLIC);
		def.interfaces(Type.of(ArraySetter.class));
		def.addEmptyConstructor(Access.PUBLIC);
		
		addSetter(def, Type.BOOLEAN.array(1));
		addSetter(def, Type.BYTE.array(1));
		addSetter(def, Type.SHORT.array(1));
		addSetter(def,  Type.CHAR.array(1));
		addSetter(def, Type.INT.array(1));
		addSetter(def, Type.LONG.array(1));
		addSetter(def, Type.FLOAT.array(1));
		addSetter(def, Type.DOUBLE.array(1));
		addSetter(def, Type.OBJECT.array(1));
		addSetter(def, Type.INT.array(2));
		addSetter(def, Type.OBJECT.array(3));
		
		var instance = (ArraySetter) TestUtils.newInstance(def, opts);
		
		var booleans = new boolean[20];
		instance.set(booleans, 19, true);
		assertTrue(booleans[19]);
		assertFalse(booleans[0]);
		
		testNumberSet(instance, byte.class);
		testNumberSet(instance, short.class);
		testNumberSet(instance, int.class);
		testNumberSet(instance, long.class);
		testNumberSet(instance, float.class);
		testNumberSet(instance, double.class);
	}
	
	interface LengthGetter {
		int direct(Object[] array);
		int field(Object[] array);
	}
	
	@ParameterizedTest
	@OptionsSource
	public void arrayLength(CompileOptions opts) throws Throwable {
		var def = ClassDef.create("fi.benjami.code4jvm.test.ArrayLength", Access.PUBLIC);
		def.interfaces(Type.of(LengthGetter.class));
		def.addEmptyConstructor(Access.PUBLIC);
		
		{
			var method = def.addMethod(Type.INT, "direct", Access.PUBLIC);
			var arg = method.arg(Type.OBJECT.array(1));
			var length = method.add(ArrayAccess.length(arg));
			method.add(Return.value(length));
		}
		
		{
			var method = def.addMethod(Type.INT, "field", Access.PUBLIC);
			var arg = method.arg(Type.OBJECT.array(1));
			var length = method.add(arg.getField(Type.INT, "length"));
			method.add(Return.value(length));
		}
		
		var instance = (LengthGetter) TestUtils.newInstance(def, opts);
		
		assertEquals(80, instance.direct(new Object[80]));
		assertEquals(20, instance.field(new Object[20]));
	}
	
	@Test
	public void arrayLengthFailures() throws Throwable {
		var def = ClassDef.create("fi.benjami.code4jvm.test.ArrayLength", Access.PUBLIC);
		def.addEmptyConstructor(Access.PUBLIC);
		
		var method = def.addMethod(Type.INT, "fail1", Access.PUBLIC);
		var arg = method.arg(Type.OBJECT.array(1));
		assertThrows(IllegalArgumentException.class, () -> arg.getField(Type.OBJECT, "length"));
		assertThrows(IllegalArgumentException.class, () -> arg.getField(Type.INT, "foo"));
	}
}
