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

/**
 * A callable unit such as a concrete method or a lambda.
 * 
 * @see Method
 * @see Lambda
 *
 */
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
	
	/**
	 * Adds a named argument to this routine.
	 * @param type Type of the argument.
	 * @param name Name of the argument.
	 * @return Value that represents the argument.
	 */
	public Value arg(Type type, String name) {
		var localVar = new LocalVar(type, true);
		localVar.name(name);
		argsAllocator.get(localVar);
		args.add(localVar);
		return localVar;
	}
	
	/**
	 * Adds an unnamed argument to this routine.
	 * @param type Type of the argument.
	 * @return Value that represents the argument.
	 */
	public Value arg(Type type) {
		return arg(type, null);
	}
	
	/**
	 * Gets an unmodifiable list of previously added arguments.
	 * @return Argument values.
	 */
	public List<Value> arguments() {
		return Collections.unmodifiableList(args);
	}
	
	public List<Type> argumentTypes() {
		return args.stream().map(Value::type).toList();
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
