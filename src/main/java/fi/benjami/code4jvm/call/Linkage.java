package fi.benjami.code4jvm.call;

/**
 * Method call linkage type.
 *
 */
public enum Linkage {

	/**
	 * Target is static method.
	 * 
	 * @implNote Compiles to {@code invokestatic}.
	 */
	STATIC,
	
	/**
	 * Target is an instance method that is not owned by an interface.
	 * 
	 * @implNote Compiles to {@code invokevirtual}.
	 */
	VIRTUAL,
	
	/**
	 * Target is an instance method that is owned by an interface.
	 * 
	 * @implNote Compiles to {@code invokeinterface}.
	 */
	INTERFACE,
	
	/**
	 * Target is a private method.
	 * 
	 * @implNote Compiles to {@code invokespecial}.
	 */
	SPECIAL,
	
	/**
	 * Target is a constructor.
	 * 
	 * @implNote Compiles to a combination of
	 * {@code new}, {@code dup} and {@code invokespecial}.
	 */
	INIT,
	
	/**
	 * Target is built-in that creates a new array.
	 * 
	 * @implNote Compiles to {@code newarray} for one-dimensional arrays
	 * of primitive types, {@code anewarray} for one-dimensional arrays
	 * of references types, or {@code multianewarray} for all multidimensional
	 * arrays.
	 */
	INIT_ARRAY,
	
	/**
	 * Target is determined dynamically by calling a bootstrap method.
	 * 
	 * @implNote Compiles to {@code invokedynamic}.
	 */
	DYNAMIC
}
