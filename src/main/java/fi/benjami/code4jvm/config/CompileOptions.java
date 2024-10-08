package fi.benjami.code4jvm.config;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import fi.benjami.code4jvm.statement.Bytecode;

/**
 * A map of compile options that can be queried by
 * {@link Bytecode bytecode generators} as well as used internally by code4jvm.
 * 
 * @see CompileOption
 *
 */
public class CompileOptions {
	
	public static final CompileOptions DEFAULT = new CompileOptions(new IdentityHashMap<>());
	
	public static Builder builder() {
		return new Builder();
	}
	
	public static class Builder {
		
		private final Map<CompileOption<?>, Object> options;
		
		private Builder() {
			this.options = new IdentityHashMap<>();
		}
		
		public <T> Builder set(CompileOption<T> option, T value) {
			options.put(option, value);
			return this;
		}
		
		public CompileOptions build() {
			return new CompileOptions(new IdentityHashMap<>(options));
		}
	}
	
	private final Map<CompileOption<?>, Object> options;
	
	private CompileOptions(Map<CompileOption<?>, Object> options) {
		this.options = options;
		
		// Check that requirements of all options are met
		for (var entry : options.entrySet()) {
			for (var requirement : entry.getKey().requirements) {
				if (entry.getValue().equals(requirement.ourValue())) {
					// Our value is the one specified in requirement, so check it
					if (!requirement.check(get(requirement.otherOption()))) {
						throw new InvalidConfigException(entry.getKey().name() + " EQUAL " + entry.getValue() + " requires "
								+ requirement.otherOption() + " " + requirement.compareOp() + " " + requirement.expected());
					}
				}
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	public <T> T get(CompileOption<T> option) {
		// To avoid computing default value multiple times, save results to our map
		return (T) options.computeIfAbsent(option, (k) -> k.defaultValue.apply(this));
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof CompileOptions opts) {
			boolean same = true;
			for (var entry : this.options.entrySet()) {
				if (!opts.get(entry.getKey()).equals(entry.getValue())) {
					same = false;
				}
			}
			for (var entry : opts.options.entrySet()) {
				if (!this.get(entry.getKey()).equals(entry.getValue())) {
					same = false;
				}
			}
			return same;
		}
		return false;
	}
	
	@Override
	public String toString() {
		var values = options.entrySet().stream()
				.sorted((a, b) -> a.getKey().name().compareTo(b.getKey().name()))
				.map(entry -> entry.getKey().name() + "=" + entry.getValue())
				.collect(Collectors.joining(","));
		
		return "CompileOptions{" + values + "}";
	}
}
