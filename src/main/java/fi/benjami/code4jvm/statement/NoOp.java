package fi.benjami.code4jvm.statement;

import fi.benjami.code4jvm.Statement;
import fi.benjami.code4jvm.block.Block;

public class NoOp implements Statement {

	public static final NoOp INSTANCE = new NoOp();
	
	private NoOp() {}
	
	@Override
	public void emitVoid(Block block) {
		// No-op
	}

}
