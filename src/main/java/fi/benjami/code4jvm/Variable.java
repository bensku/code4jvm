package fi.benjami.code4jvm;

import fi.benjami.code4jvm.internal.LocalVar;

public interface Variable extends Value {
	
	/**
	 * Creates a variable that has no value until it is {@link #set(Value) set}.
	 * 
	 * <p>Unbound variables are not valid values until they receive a value.
	 * Misuse will result in {@link UninitializedValueException}s.
	 * @param type Type of the unbound variable.
	 * @return A new variable that does not yet have a value.
	 */
	static Variable createUnbound(Type type) {
		return new LocalVar(type);
	}
	
	/**
	 * Creates a statement that sets a value to this variable.
	 * @param value Value to set to this.
	 * @return The statement.
	 */
	Statement set(Value value);
	
}
