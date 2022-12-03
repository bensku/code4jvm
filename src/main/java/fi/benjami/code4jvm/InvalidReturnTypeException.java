package fi.benjami.code4jvm;

import fi.benjami.code4jvm.block.Routine;
import fi.benjami.code4jvm.statement.Return;

/**
 * Thrown when a {@link Return return} is used with value that doesn't match
 * the {@link Routine#returnType() return type} of the parent method.
 *
 */
public class InvalidReturnTypeException extends RuntimeException {
	
	private static final long serialVersionUID = 1L;
	
	public InvalidReturnTypeException(Value value, Type expectedType) {
		super("tried to return " + value + ", but parent method has return type of " + expectedType);
	}
}
