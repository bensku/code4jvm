package fi.benjami.code4jvm.internal;

import java.util.Optional;

import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.Value;

/**
 * Special value that represents what is already on stack. This should be NEVER
 * produced as an output.
 * 
 * @see Value#stackTop(Type)
 *
 */
public record StackTop(
		Type type
) implements Value {

	@Override
	public Type type() {
		return type;
	}

	@Override
	public Optional<String> name() {
		return Optional.empty();
	}

	@Override
	public Value original() {
		return this;
	}
}
