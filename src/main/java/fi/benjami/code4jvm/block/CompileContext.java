package fi.benjami.code4jvm.block;

import org.objectweb.asm.MethodVisitor;

import fi.benjami.code4jvm.call.FixedCallTarget;
import fi.benjami.code4jvm.config.CompileOptions;
import fi.benjami.code4jvm.typedef.ClassDef;

/**
 * Context for compiling a single method.
 *
 */
public final class CompileContext {
	
	private final ClassDef owner;
	private final FixedCallTarget method;
	private final MethodVisitor asm;
	private final CompileOptions options;
	private final StackManager stack;
	
	CompileContext(ClassDef owner, FixedCallTarget method, MethodVisitor asm, CompileOptions options) {
		this.owner = owner;
		this.method = method;
		this.asm = asm;
		this.options = options;
		this.stack = new StackManager(this);
	}

	/**
	 * Type that is being compiled.
	 */
	public ClassDef owner() {
		return owner;
	}

	/**
	 * Call target that refers to the method that is being compiled.
	 */
	public FixedCallTarget method() {
		return method;
	}

	/**
	 * ASM visitor used for emitting method bytecode.
	 */
	public MethodVisitor asm() {
		return asm;
	}

	/**
	 * Compiler options used for the class that the method is part of.
	 */
	public CompileOptions options() {
		return options;
	}

	/**
	 * Tools for managing JVM stack.
	 */
	public StackManager stack() {
		return stack;
	}
	
}
