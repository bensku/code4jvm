package fi.benjami.code4jvm.block;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import fi.benjami.code4jvm.Expression;
import fi.benjami.code4jvm.Statement;
import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.Value;
import fi.benjami.code4jvm.block.Block.AddExpression;
import fi.benjami.code4jvm.internal.LocalVar;
import fi.benjami.code4jvm.internal.SlotAllocator;

public class Routine {
	
	private final Block block;
	
	private final Type returnType;
	final List<LocalVar> args;
	final SlotAllocator argsAllocator;
	
	Routine(Block block, Type returnType) {
		this.block = block;
		this.returnType = returnType;
		this.args = new ArrayList<>();
		this.argsAllocator = new SlotAllocator(null);
	}
	
	public Block block() {
		return block;
	}
	
	public Type returnType() {
		return returnType;
	}
	
	public Value arg(Type type, String name) {
		var localVar = new LocalVar(type, block);
		localVar.initialized = true; // Arguments are always initialized (but can be null)
		localVar.name(name);
		argsAllocator.get(localVar);
		args.add(localVar);
		return localVar;
	}
	
	public Value arg(Type type) {
		return arg(type, null);
	}
	
	public List<Value> arguments() {
		return Collections.unmodifiableList(args);
	}
	
	public void add(Statement stmt) {
		block.add(stmt);
	}
	
	public AddExpression add(Expression expr) {
		return block.add(expr);
	}
	
	public void add(Block block) {
		this.block.add(block);
	}
	
}
