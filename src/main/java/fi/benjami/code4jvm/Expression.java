package fi.benjami.code4jvm;

import fi.benjami.code4jvm.block.Block;

public interface Expression extends Statement {
	
	Value emitValue(Block block);
	
	default void emitVoid(Block block) {
		emitValue(block);
	}
}
