package fi.benjami.code4jvm.test;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class TestUtils {

	public static final Object newInstance(MethodHandles.Lookup lookup) throws Throwable {
		return lookup.findConstructor(lookup.lookupClass(), MethodType.methodType(void.class)).invoke();
	}
}
