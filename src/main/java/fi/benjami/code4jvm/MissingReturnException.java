package fi.benjami.code4jvm;

import fi.benjami.code4jvm.block.Method;
import fi.benjami.code4jvm.statement.Return;
import fi.benjami.code4jvm.statement.Throw;
import fi.benjami.code4jvm.typedef.ClassDef;

/**
 * Thrown when a method that is not guaranteed to {@link Return return} or
 * {@link Throw throw} is {@link ClassDef#compile() compiled}. JVM bytecode
 * verifier requires that all methods do this. Unlike Java, code4jvm does not
 * automatically add returns to void methods.
 *
 */
public class MissingReturnException extends RuntimeException {

	private static final long serialVersionUID = 1L;
	
	public MissingReturnException(Method method) {
		super("missing return/throw in method:\n" + method);
	}

}
