package fi.benjami.code4jvm.internal;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import org.objectweb.asm.Label;

import fi.benjami.code4jvm.block.Block;
import fi.benjami.code4jvm.statement.Jump;

public class SharedSecrets {

	public static BiFunction<Block, Jump.Target, Label> LABEL_GETTER;
	public static BiConsumer<Block, Node> NODE_APPENDER;
}
