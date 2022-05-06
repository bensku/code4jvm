package fi.benjami.code4jvm.test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.invoke.MethodHandles;

import org.junit.jupiter.api.Test;

import fi.benjami.code4jvm.ClassDef;
import fi.benjami.code4jvm.Condition;
import fi.benjami.code4jvm.Constant;
import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.block.Block;
import fi.benjami.code4jvm.block.Method;
import fi.benjami.code4jvm.flag.Access;
import fi.benjami.code4jvm.statement.Jump;
import fi.benjami.code4jvm.statement.Return;

public class ConditionTest {

	private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

	interface BooleanChecker {
		boolean isTrue(boolean v);
		boolean isFalse(boolean v);
	}
	
	@Test
	public void booleans() throws Throwable {
		var def = ClassDef.create("fi.benjami.code4jvm.test.Booleans", Access.PUBLIC);
		def.addEmptyConstructor(Access.PUBLIC);
		def.interfaces(Type.of(BooleanChecker.class));
		
		{			
			var method = def.addMethod(Type.BOOLEAN, "isTrue", Access.PUBLIC);
			var arg = method.arg(Type.BOOLEAN);
			setupJump(method, Condition.isTrue(arg));
		}
		{
			var method = def.addMethod(Type.BOOLEAN, "isFalse", Access.PUBLIC);
			var arg = method.arg(Type.BOOLEAN);
			setupJump(method, Condition.isFalse(arg));
		}
		
		var lookup = LOOKUP.defineHiddenClass(def.compile(), true);
		var instance = (BooleanChecker) TestUtils.newInstance(lookup);
		
		assertTrue(instance.isTrue(true));
		assertFalse(instance.isTrue(false));
		assertFalse(instance.isFalse(true));
		assertTrue(instance.isFalse(false));
	}
	
	interface ObjectEqualChecker {
		boolean equal(Object a, Object b);
		boolean notEqual(Object a, Object b);
	}
	
	@Test
	public void refEquality() throws Throwable {
		assertThrows(IllegalArgumentException.class, () -> Condition.refEqual(Constant.of(false), Constant.of(false)));
		
		var def = ClassDef.create("fi.benjami.code4jvm.test.RefEquality", Access.PUBLIC);
		def.addEmptyConstructor(Access.PUBLIC);
		def.interfaces(Type.of(ObjectEqualChecker.class));
		
		{			
			var method = def.addMethod(Type.BOOLEAN, "equal", Access.PUBLIC);
			var a = method.arg(Type.OBJECT);
			var b = method.arg(Type.OBJECT);
			setupJump(method, Condition.refEqual(a, b));
		}
		{
			var method = def.addMethod(Type.BOOLEAN, "notEqual", Access.PUBLIC);
			var a = method.arg(Type.OBJECT);
			var b = method.arg(Type.OBJECT);
			setupJump(method, Condition.refEqual(a, b).not());
		}
		
		var lookup = LOOKUP.defineHiddenClass(def.compile(), true);
		var instance = (ObjectEqualChecker) TestUtils.newInstance(lookup);
		
		var obj = new Object();
		record HasEquals(String text) {}
		assertTrue(instance.equal(obj, obj));
		assertFalse(instance.equal(new Object(), new Object()));
		assertFalse(instance.equal(new HasEquals("foo"), new HasEquals("foo")));
		assertFalse(instance.notEqual(obj, obj));
		assertTrue(instance.notEqual(new Object(), new Object()));
		assertTrue(instance.notEqual(new HasEquals("foo"), new HasEquals("foo")));
	}
	
	@Test
	public void objectEquality() throws Throwable {
		var def = ClassDef.create("fi.benjami.code4jvm.test.ObjectEquality", Access.PUBLIC);
		def.addEmptyConstructor(Access.PUBLIC);
		def.interfaces(Type.of(ObjectEqualChecker.class));
		
		{			
			var method = def.addMethod(Type.BOOLEAN, "equal", Access.PUBLIC);
			var a = method.arg(Type.OBJECT);
			var b = method.arg(Type.OBJECT);
			setupJump(method, Condition.equal(a, b));
		}
		{
			var method = def.addMethod(Type.BOOLEAN, "notEqual", Access.PUBLIC);
			var a = method.arg(Type.OBJECT);
			var b = method.arg(Type.OBJECT);
			setupJump(method, Condition.equal(a, b).not());
		}
		
		var lookup = LOOKUP.defineHiddenClass(def.compile(), true);
		var instance = (ObjectEqualChecker) TestUtils.newInstance(lookup);
		
		var obj = new Object();
		record HasEquals(String text) {}
		assertTrue(instance.equal(obj, obj));
		assertFalse(instance.equal(new Object(), new Object()));
		assertTrue(instance.equal(new HasEquals("foo"), new HasEquals("foo")));
		assertFalse(instance.notEqual(obj, obj));
		assertTrue(instance.notEqual(new Object(), new Object()));
		assertFalse(instance.notEqual(new HasEquals("foo"), new HasEquals("foo")));
	}
	
	record CompTest(int value) implements Comparable<CompTest> {

		@Override
		public int compareTo(CompTest o) {
			return Integer.compare(value, o.value);
		}
		
	}
	
	interface ObjectCompareChecker {
		boolean lessThan(CompTest a, CompTest b);
		boolean lessOrEq(CompTest a, CompTest b);
		boolean greaterThan(CompTest a, CompTest b);
		boolean greaterOrEq(CompTest a, CompTest b);
	}
	
	@Test
	public void objectComparisons() throws Throwable {
		var def = ClassDef.create("fi.benjami.code4jvm.test.ObjectComparisons", Access.PUBLIC);
		def.addEmptyConstructor(Access.PUBLIC);
		def.interfaces(Type.of(ObjectCompareChecker.class));
		
		{			
			var method = def.addMethod(Type.BOOLEAN, "lessThan", Access.PUBLIC);
			var a = method.arg(Type.of(CompTest.class));
			var b = method.arg(Type.of(CompTest.class));
			setupJump(method, Condition.lessThan(a, b));
		}
		{			
			var method = def.addMethod(Type.BOOLEAN, "lessOrEq", Access.PUBLIC);
			var a = method.arg(Type.of(CompTest.class));
			var b = method.arg(Type.of(CompTest.class));
			setupJump(method, Condition.lessOrEqual(a, b));
		}
		{			
			var method = def.addMethod(Type.BOOLEAN, "greaterThan", Access.PUBLIC);
			var a = method.arg(Type.of(CompTest.class));
			var b = method.arg(Type.of(CompTest.class));
			setupJump(method, Condition.greaterThan(a, b));
		}
		{			
			var method = def.addMethod(Type.BOOLEAN, "greaterOrEq", Access.PUBLIC);
			var a = method.arg(Type.of(CompTest.class));
			var b = method.arg(Type.of(CompTest.class));
			setupJump(method, Condition.greaterOrEqual(a, b));
		}
		
		var lookup = LOOKUP.defineHiddenClass(def.compile(), true);
		var instance = (ObjectCompareChecker) TestUtils.newInstance(lookup);
		
		assertTrue(instance.lessThan(new CompTest(0), new CompTest(1)));
		assertTrue(instance.lessOrEq(new CompTest(0), new CompTest(1)));
		assertFalse(instance.greaterOrEq(new CompTest(0), new CompTest(1)));
		assertFalse(instance.greaterThan(new CompTest(0), new CompTest(1)));
		
		assertFalse(instance.lessThan(new CompTest(10), new CompTest(10)));
		assertTrue(instance.lessOrEq(new CompTest(10), new CompTest(10)));
		assertTrue(instance.greaterOrEq(new CompTest(10), new CompTest(10)));
		assertFalse(instance.greaterThan(new CompTest(10), new CompTest(10)));
		
		assertFalse(instance.lessThan(new CompTest(100), new CompTest(50)));
		assertFalse(instance.lessOrEq(new CompTest(100), new CompTest(50)));
		assertTrue(instance.greaterOrEq(new CompTest(100), new CompTest(50)));
		assertTrue(instance.greaterThan(new CompTest(100), new CompTest(50)));
	}
	
	interface IntEqualChecker {
		boolean equal(int a, int b);
		boolean notEqual(int a, int b);
	}
	
	@Test
	public void intEquality() throws Throwable {
		// JVM treats many primitives (almost) like ints
		// Our code path is same for boolean, byte, short, char and int
		var def = ClassDef.create("fi.benjami.code4jvm.test.IntEquality", Access.PUBLIC);
		def.addEmptyConstructor(Access.PUBLIC);
		def.interfaces(Type.of(IntEqualChecker.class));
		
		{			
			var method = def.addMethod(Type.BOOLEAN, "equal", Access.PUBLIC);
			var a = method.arg(Type.INT);
			var b = method.arg(Type.INT);
			setupJump(method, Condition.equal(a, b));
		}
		{
			var method = def.addMethod(Type.BOOLEAN, "notEqual", Access.PUBLIC);
			var a = method.arg(Type.INT);
			var b = method.arg(Type.INT);
			setupJump(method, Condition.equal(a, b).not());
		}
		
		var lookup = LOOKUP.defineHiddenClass(def.compile(), true);
		var instance = (IntEqualChecker) TestUtils.newInstance(lookup);
		
		assertTrue(instance.equal(0, 0));
		assertFalse(instance.notEqual(0, 0));
		assertTrue(instance.equal(100_000, 100_000));
		assertFalse(instance.notEqual(100_000, 100_000));
		assertTrue(instance.equal(-100_000, -100_000));
		assertFalse(instance.notEqual(-100_000, -100_000));
		
		assertFalse(instance.equal(-10, 10));
		assertTrue(instance.notEqual(-10, 10));
	}
	
	interface IntCompareChecker {
		boolean lessThan(int a, int b);
		boolean lessOrEq(int a, int b);
		boolean greaterThan(int a, int b);
		boolean greaterOrEq(int a, int b);
	}
	
	@Test
	public void intComparisons() throws Throwable {
		// JVM treats many primitives (almost) like ints
		// Our code path is same for boolean, byte, short, char and int
		var def = ClassDef.create("fi.benjami.code4jvm.test.IntComparisons", Access.PUBLIC);
		def.addEmptyConstructor(Access.PUBLIC);
		def.interfaces(Type.of(IntCompareChecker.class));
		
		{			
			var method = def.addMethod(Type.BOOLEAN, "lessThan", Access.PUBLIC);
			var a = method.arg(Type.INT);
			var b = method.arg(Type.INT);
			setupJump(method, Condition.lessThan(a, b));
		}
		{			
			var method = def.addMethod(Type.BOOLEAN, "lessOrEq", Access.PUBLIC);
			var a = method.arg(Type.INT);
			var b = method.arg(Type.INT);
			setupJump(method, Condition.lessOrEqual(a, b));
		}
		{			
			var method = def.addMethod(Type.BOOLEAN, "greaterThan", Access.PUBLIC);
			var a = method.arg(Type.INT);
			var b = method.arg(Type.INT);
			setupJump(method, Condition.greaterThan(a, b));
		}
		{			
			var method = def.addMethod(Type.BOOLEAN, "greaterOrEq", Access.PUBLIC);
			var a = method.arg(Type.INT);
			var b = method.arg(Type.INT);
			setupJump(method, Condition.greaterOrEqual(a, b));
		}
		
		var lookup = LOOKUP.defineHiddenClass(def.compile(), true);
		var instance = (IntCompareChecker) TestUtils.newInstance(lookup);
		
		assertTrue(instance.lessThan(0, 1));
		assertTrue(instance.lessOrEq(0, 1));
		assertFalse(instance.greaterOrEq(0, 1));
		assertFalse(instance.greaterThan(0, 1));
		
		assertFalse(instance.lessThan(10, 10));
		assertTrue(instance.lessOrEq(10, 10));
		assertTrue(instance.greaterOrEq(10, 10));
		assertFalse(instance.greaterThan(10, 10));
		
		assertFalse(instance.lessThan(100, 50));
		assertFalse(instance.lessOrEq(100, 50));
		assertTrue(instance.greaterOrEq(100, 50));
		assertTrue(instance.greaterThan(100, 50));
	}
	
	interface LongEqualChecker {
		boolean equal(long a, long b);
		boolean notEqual(long a, long b);
	}
	
	@Test
	public void longEquality() throws Throwable {
		// Long shares most of the code path with float and double
		var def = ClassDef.create("fi.benjami.code4jvm.test.LongEquality", Access.PUBLIC);
		def.addEmptyConstructor(Access.PUBLIC);
		def.interfaces(Type.of(LongEqualChecker.class));
		
		{			
			var method = def.addMethod(Type.BOOLEAN, "equal", Access.PUBLIC);
			var a = method.arg(Type.LONG);
			var b = method.arg(Type.LONG);
			setupJump(method, Condition.equal(a, b));
		}
		{
			var method = def.addMethod(Type.BOOLEAN, "notEqual", Access.PUBLIC);
			var a = method.arg(Type.LONG);
			var b = method.arg(Type.LONG);
			setupJump(method, Condition.equal(a, b).not());
		}
		
		var lookup = LOOKUP.defineHiddenClass(def.compile(), true);
		var instance = (LongEqualChecker) TestUtils.newInstance(lookup);
		
		assertTrue(instance.equal(0, 0));
		assertFalse(instance.notEqual(0, 0));
		assertTrue(instance.equal(100_000, 100_000));
		assertFalse(instance.notEqual(100_000, 100_000));
		assertTrue(instance.equal(-100_000, -100_000));
		assertFalse(instance.notEqual(-100_000, -100_000));
		
		assertFalse(instance.equal(-10, 10));
		assertTrue(instance.notEqual(-10, 10));
		
		assertFalse(instance.equal(Long.MAX_VALUE, Long.MIN_VALUE));
		assertTrue(instance.notEqual(Long.MAX_VALUE, Long.MIN_VALUE));
	}
	
	interface LongCompareChecker {
		boolean lessThan(long a, long b);
		boolean lessOrEq(long a, long b);
		boolean greaterThan(long a, long b);
		boolean greaterOrEq(long a, long b);
	}
	
	@Test
	public void longComparisons() throws Throwable {
		// Long shares most of the code path with float and double
		var def = ClassDef.create("fi.benjami.code4jvm.test.LongComparisons", Access.PUBLIC);
		def.addEmptyConstructor(Access.PUBLIC);
		def.interfaces(Type.of(LongCompareChecker.class));
		
		{			
			var method = def.addMethod(Type.BOOLEAN, "lessThan", Access.PUBLIC);
			var a = method.arg(Type.LONG);
			var b = method.arg(Type.LONG);
			setupJump(method, Condition.lessThan(a, b));
		}
		{			
			var method = def.addMethod(Type.BOOLEAN, "lessOrEq", Access.PUBLIC);
			var a = method.arg(Type.LONG);
			var b = method.arg(Type.LONG);
			setupJump(method, Condition.lessOrEqual(a, b));
		}
		{			
			var method = def.addMethod(Type.BOOLEAN, "greaterThan", Access.PUBLIC);
			var a = method.arg(Type.LONG);
			var b = method.arg(Type.LONG);
			setupJump(method, Condition.greaterThan(a, b));
		}
		{			
			var method = def.addMethod(Type.BOOLEAN, "greaterOrEq", Access.PUBLIC);
			var a = method.arg(Type.LONG);
			var b = method.arg(Type.LONG);
			setupJump(method, Condition.greaterOrEqual(a, b));
		}
		
		var lookup = LOOKUP.defineHiddenClass(def.compile(), true);
		var instance = (LongCompareChecker) TestUtils.newInstance(lookup);
		
		assertTrue(instance.lessThan(0, 1));
		assertTrue(instance.lessOrEq(0, 1));
		assertFalse(instance.greaterOrEq(0, 1));
		assertFalse(instance.greaterThan(0, 1));
		
		assertFalse(instance.lessThan(10, 10));
		assertTrue(instance.lessOrEq(10, 10));
		assertTrue(instance.greaterOrEq(10, 10));
		assertFalse(instance.greaterThan(10, 10));
		
		assertFalse(instance.lessThan(100, 50));
		assertFalse(instance.lessOrEq(100, 50));
		assertTrue(instance.greaterOrEq(100, 50));
		assertTrue(instance.greaterThan(100, 50));
	}

	private void setupJump(Method method, Condition condition) {
		var outer = method.block();
		var inner = Block.create();
		inner.add(Jump.to(inner, Jump.Target.END, condition));
		inner.add(Return.value(Constant.of(false)));
		outer.add(inner);
		outer.add(Return.value(Constant.of(true)));
	}
}
