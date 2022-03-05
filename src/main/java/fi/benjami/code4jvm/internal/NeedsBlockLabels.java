package fi.benjami.code4jvm.internal;

import org.objectweb.asm.Label;

import fi.benjami.code4jvm.Block;

public interface NeedsBlockLabels {
	
	public static int NEED_START = 1, NEED_END = 1 << 1;

	Block targetBlock();
	
	int setLabels(Label start, Label end);
}
