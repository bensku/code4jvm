package fi.benjami.code4jvm.lua.ffi;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks the annotated Java method as callable from Lua. It becomes a target
 * of a {@link JavaFunction Java function} with the given
 * {@link #value() name}. If a same class has multiple exported methods with
 * same function name, they all become targets of it. This allows overloading
 * functions to e.g. support different numbers of arguments or multiple
 * data types.
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface LuaExport {

	/**
	 * Name of the function.
	 */
	String value();
}
