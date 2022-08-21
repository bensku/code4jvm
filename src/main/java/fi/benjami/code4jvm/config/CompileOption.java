package fi.benjami.code4jvm.config;

import java.util.function.Function;

/**
 * A key to store values in {@link CompileOptions}.
 *
 * @param <V> Type of value this option can receive.
 */
public class CompileOption<V> {
	
	private final String name;
	final Function<CompileOptions, V> defaultValue;
	final Requirement<V, ?>[] requirements;
	
	@SafeVarargs
	public CompileOption(String name, Function<CompileOptions, V> defaultValue, Requirement<V, ?>... requirements) {
		this.name = name;
		this.defaultValue = defaultValue;
		this.requirements = requirements;
	}
	
	@SafeVarargs
	public CompileOption(String name, V defaultValue, Requirement<V, ?>... requirements) {
		this(name, (options) -> defaultValue, requirements);
	}
	
	public String name() {
		return name;
	}
}
