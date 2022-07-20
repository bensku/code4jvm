package fi.benjami.code4jvm;

import fi.benjami.code4jvm.block.Block;

public class UninitializedValueException extends RuntimeException {

	private static final long serialVersionUID = 1L;
	
	public UninitializedValueException(Value value, Block block) {
		super(value + " is uninitialized at " + block);
	}

}
