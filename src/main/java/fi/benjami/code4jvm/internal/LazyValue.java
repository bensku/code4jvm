package fi.benjami.code4jvm.internal;

import java.util.Optional;

import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.Value;
import fi.benjami.code4jvm.block.Block;
import fi.benjami.code4jvm.structure.TryBlock;

/**
 * Sometimes, it is convenient to return a value from API before it really
 * exists. As long as the value is guaranteed to exist before bytecode is
 * generated, this can be used for such situations.
 * 
 * @see TryBlock
 * @see ValueTools
 *
 */
public class LazyValue implements Value {

	private final Type type;
	
	/**
	 * The value that is set lazily. This should be non-null when bytecode is
	 * emitted!
	 */
	public Value value;
	
	public LazyValue(Type type) {
		this.type = type;
	}
	
	@Override
	public Type type() {
		return type;
	}

	@Override
	public Optional<Block> parentBlock() {
		return Optional.empty(); // TODO should this be supported?
	}

	@Override
	public Optional<String> name() {
		return Optional.empty();
	}

}
