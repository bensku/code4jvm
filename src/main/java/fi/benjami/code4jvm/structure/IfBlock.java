package fi.benjami.code4jvm.structure;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import fi.benjami.code4jvm.Block;
import fi.benjami.code4jvm.Condition;
import fi.benjami.code4jvm.Statement;
import fi.benjami.code4jvm.statement.Jump;

public class IfBlock implements Statement {
	
	private record Case(
			Condition condition,
			Block block
	) {}
	
	public static IfBlock with(Condition condition) {
		return new IfBlock(condition);
	}
	
	private final List<Case> cases;
	private Block fallback;
		
	private IfBlock(Condition condition) {
		this.cases = new ArrayList<>();
		cases.add(new Case(condition, Block.create()));
	}

	public Block then() {
		return cases.get(0).block;
	}
	
	public IfBlock then(Consumer<Block> callback) {
		callback.accept(then());
		return this;
	}
	
	public Block elseIf(Condition condition) {
		var block = Block.create();
		cases.add(new Case(condition, block));
		return block;
	}
	
	public IfBlock elseIf(Condition condition, Consumer<Block> callback) {
		callback.accept(elseIf(condition));
		return this;
	}
	
	public Block orElse() {
		var block = Block.create();
		fallback = block;
		return block;
	}
	
	public IfBlock orElse(Consumer<Block> callback) {
		callback.accept(orElse());
		return this;
	}

	@Override
	public void emitVoid(Block block) {
		var root = Block.create();
		for (int i = 0; i < cases.size(); i++) {
			var option = cases.get(i);
			var edge = Block.create();
			// Skip this case if condition is not met
			edge.add(Jump.to(edge, Jump.Target.END, option.condition().not()));
			edge.add(option.block);
			// If condition was met, skip rest of cases and fallback
			if (i != cases.size() - 1 || fallback != null) {
				edge.add(Jump.to(root, Jump.Target.END));
			} // else: nothing after this, jump unnecessary
			root.add(edge);
		}
		if (fallback != null) {
			// If this is reached, everything else has been jumped over
			root.add(fallback);
		}
		block.add(root);
	}
}
