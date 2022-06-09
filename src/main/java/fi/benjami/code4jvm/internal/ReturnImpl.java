package fi.benjami.code4jvm.internal;

import fi.benjami.code4jvm.Statement;
import fi.benjami.code4jvm.Value;
import fi.benjami.code4jvm.block.Block;

public record ReturnImpl(
		Value value
) implements Statement {

	@Override
	public void emitVoid(Block block) {
		throw new UnsupportedOperationException("return special case");
	}

}
