package fi.benjami.code4jvm.call;

import fi.benjami.code4jvm.Expression;
import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.Value;

/**
 * A target for method calls, i.e. a method or dynamic target.
 *
 */
public abstract class CallTarget {
	
	private final String name;
	
	CallTarget(String name) {
		this.name = name;
	}
	
	public String name() {
		return name;
	}
	
	/**
	 * Creates a call to this target.
	 * @param returnType Type of returned value.
	 * @param args Argument values for the call.
	 * @return Method call expression.
	 */
	public abstract Expression call(Type returnType, Value... args);
	
}
