package fi.benjami.code4jvm;

public interface Expression extends Statement {
	
	Value emitValue(Block block);
	
	default void emitVoid(Block block) {
		emitValue(block);
	}
}
