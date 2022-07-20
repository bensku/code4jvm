package fi.benjami.code4jvm.block;

import java.util.Optional;

import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.Variable;
import fi.benjami.code4jvm.util.TypeCheck;

public class ReturnRedirect {

	/**
	 * Block where the return is redirected.
	 */
	private final Block target;
	
	/**
	 * Local variable where return value should be stored. Null if method
	 * returns void.
	 */
	private final Variable valueHolder;
	
	public ReturnRedirect(Block target, Variable valueHolder) {
		TypeCheck.mustBe(valueHolder, Type.METHOD_RETURN_TYPE);
		this.target = target;
		this.valueHolder = valueHolder;
	}
	
	public Block target() {
		return target;
	}
	
	public Optional<Variable> valueHolder() {
		return Optional.ofNullable(valueHolder);
	}
}
