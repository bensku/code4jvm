package fi.benjami.code4jvm.lua.ffi;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a Java method as a fallback target for its
 * {@link LuaExport exported function}. This means that it will be executed
 * when the arguments provided were not valid for any other target.
 * 
 * <p>Fallback method must accept zero or more Object arguments.
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Fallback {

}
