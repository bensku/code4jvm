package fi.benjami.code4jvm;

import fi.benjami.code4jvm.block.Block;
import fi.benjami.code4jvm.internal.LocalVar;

public interface Variable extends Value {
	
	/**
	 * Creates a variable that has no value until it is {@link #set(Value) set}.
	 * 
	 * <p>Variables cannot be used before they receive a value. If this is done
	 * incorrectly, {@link UninitializedValueException} will be thrown when
	 * compiling the method.
	 * @param type Type of the variable.
	 * @param name Name of the variable.
	 * @return A new variable that does not yet have a value.
	 * @see Block#add(Variable, Expression)
	 */
	static Variable create(Type type, String name) {
		return new LocalVar(type, name);
	}
	
	/**
	 * Creates a variable that has no value until it is {@link #set(Value) set}.
	 * 
	 * <p>Variables cannot be used before they receive a value. If this is done
	 * incorrectly, {@link UninitializedValueException} will be thrown when
	 * compiling the method.
	 * @param type Type of the variable.
	 * @return A new variable that does not yet have a value.
	 * @see Block#add(Variable, Expression)
	 */
	static Variable create(Type type) {
		return new LocalVar(type, null);
	}
	
	/**
	 * Creates a statement that sets a value to this variable.
	 * @param value Value to set to this.
	 * @return The statement.
	 */
	Statement set(Value value);
	
}
