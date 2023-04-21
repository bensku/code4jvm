package fi.benjami.parserkit.parser.internal;

import java.util.function.Function;

import fi.benjami.code4jvm.Condition;
import fi.benjami.code4jvm.Constant;
import fi.benjami.code4jvm.Expression;
import fi.benjami.code4jvm.Statement;
import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.Value;
import fi.benjami.code4jvm.Variable;
import fi.benjami.code4jvm.block.Block;
import fi.benjami.code4jvm.statement.BitOp;
import fi.benjami.code4jvm.statement.Jump;
import fi.benjami.code4jvm.structure.IfBlock;
import fi.benjami.parserkit.lexer.TokenizedText;
import fi.benjami.parserkit.parser.NodeRegistry;
import fi.benjami.parserkit.parser.ast.AstNode;

public class NodeCache {
	
	public static final Type STORAGE = Type.of(Storage.class);
	
	public static final NodeCache NO_CACHE = new NodeCache(null, Constant.nullValue(STORAGE));
	
	public static class Storage {
		public AstNode node;
		public long nodeMask;
		public TokenizedText.View viewAfter;
	}
	
	private final NodeRegistry registry;
	private final Value storage;
	
	public NodeCache(NodeRegistry registry, Value storage) {
		this.registry = registry;
		this.storage = storage;
	}
	
	public Value storage() {
		return this == NO_CACHE ? Constant.nullValue(STORAGE) : storage;
	}

	public Statement setCache(Class<? extends AstNode> type, Value node, Value viewAfter) {
		var mask = 1L << registry.getTypeId(type);
		return outer -> {
			var block = Block.create();
			block.add(Jump.to(block, Jump.Target.END, Condition.isNull(storage)));
			block.add(storage.putField("node", node.asType(ParserGenerator.AST_NODE)));
			block.add(storage.putField("nodeMask", Constant.of(mask)));
			block.add(storage.putField("viewAfter", viewAfter));
			outer.add(block);
		};
	}
	
	@SafeVarargs
	public final Function<Block, Condition> checkIsCached(Class<? extends AstNode>... types) {
		if (this == NO_CACHE) {
			return block -> Condition.always(false);
		}
		
		var allowedMask = 0L;
		for (var type : types) {
			allowedMask |= 1L << registry.getTypeId(type);
		}
		var mask = allowedMask;
		return block -> {
			var cachedNodeMask = Variable.create(Type.LONG);
			block.add(cachedNodeMask.set(Constant.of(0L)));
			var nullTest = new IfBlock();
			nullTest.branch(Condition.isNull(storage).not(), inner -> {
				var nodeMask = inner.add(storage.getField(Type.LONG, "nodeMask"));
				inner.add(cachedNodeMask.set(nodeMask));
			});
			block.add(nullTest);
			
			var result = block.add(BitOp.and(Constant.of(mask), cachedNodeMask));
			return Condition.equal(result, Constant.of(0L)).not();
		};
	}
	
	public Expression cachedNode() {
		return storage.getField(ParserGenerator.AST_NODE, "node");
	}
	
	public Expression viewAfter() {
		return storage.getField(ParserGenerator.TOKEN_VIEW, "viewAfter");
	}
	
	public Statement clearCache() {
		return storage.putField("nodeMask", Constant.of(0L));
	}
}
