package fi.benjami.code4jvm.lua.compiler;

import java.lang.invoke.MethodHandles;

import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.call.CallTarget;
import fi.benjami.code4jvm.call.FixedCallTarget;

/**
 * Support code for custom class data dynamic constants.
 * JDK already has {@link MethodHandles#classDataAt(java.lang.invoke.MethodHandles.Lookup, String, Class, int) a form of this},
 * but it only works with actual JDK class data. That, unfortunately, is only
 * available for hidden classes, which we can't use because they're invisible
 * in stack frames...
 *
 */
public class ClassData {

	static final String FIELD_NAME = "$CONSTANTS";
	static final FixedCallTarget BOOTSTRAP = CallTarget.staticMethod(Type.of(ClassData.class), Type.OBJECT, "get",
			Type.of(MethodHandles.Lookup.class), Type.STRING, Type.of(Class.class), Type.INT);
	
	public static Object get(MethodHandles.Lookup lookup, String ignoredName, Class<?> type, int index) {
		Object[] array;
		try {
			array = (Object[]) lookup.findStaticGetter(lookup.lookupClass(), FIELD_NAME, Object[].class)
				.invokeExact();
		} catch (Throwable e) {
			throw new AssertionError(e);
		}
		return array[index];
	}
}
