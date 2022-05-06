package fi.benjami.code4jvm.internal;

import fi.benjami.code4jvm.block.Block;

public record BlockNode(
		Block block
) implements Node {}
