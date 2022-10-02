package fi.benjami.code4jvm;

import fi.benjami.code4jvm.block.Block;
import fi.benjami.code4jvm.block.Method;

public class UninitializedValueException extends RuntimeException {

	private static final long serialVersionUID = 1L;
	
	public UninitializedValueException(Value value, Block block, Method method) {
		super(value + " is uninitialized in method:\n" + method);
	}

}
