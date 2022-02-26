package fi.benjami.code4jvm.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;

import fi.benjami.code4jvm.ClassDef;
import fi.benjami.code4jvm.flag.Access;
import fi.benjami.code4jvm.statement.Arithmetic;
import fi.benjami.code4jvm.statement.Return;

public class ArithmeticTest {

	private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

	@Test
	public void addition() throws Throwable {
		var def = ClassDef.create("fi.benjami.code4jvm.test.AddNumbers", Access.PUBLIC);
		def.addEmptyConstructor(Access.PUBLIC);
		
		var method = def.addMethod(Type.INT_TYPE, "sum", Access.PUBLIC);
		var a = method.arg(Type.INT_TYPE);
		var b = method.arg(Type.INT_TYPE);
		var sum = method.add(Arithmetic.add(a, b)).value();
		method.add(Return.value(sum));
		
		var lookup = LOOKUP.defineHiddenClass(def.compile(), true);
		assertEquals(10, lookup.findVirtual(lookup.lookupClass(),
				"sum", MethodType.methodType(int.class, int.class, int.class))
				.invoke(TestUtils.newInstance(lookup), 5, 5));
	}
}
