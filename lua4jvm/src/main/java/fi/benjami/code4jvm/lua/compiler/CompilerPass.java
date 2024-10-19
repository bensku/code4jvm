package fi.benjami.code4jvm.lua.compiler;

public enum CompilerPass {

	/**
	 * In this phase, lua4jvm IR is generated.
	 */
	IR_GEN,
	
	/**
	 * Return tracking, to determine if lua4jvm has to insert empty returns.
	 */
	RETURN_TRACKING,
	
	/**
	 * Variable flagging, based on e.g. their mutability.
	 */
	VARIABLE_TRACING,
	
	/**
	 * In analysis pass, types that can be statically inferred are inferred
	 * to generate better code later.
	 */
	TYPE_ANALYSIS,
	
	/**
	 * Code generation. In this pass, code4jvm IR is generated. Actual bytecode
	 * generation is done later by code4jvm, which has its own set of internal
	 * passes.
	 */
	CODEGEN
	;
	
	private static final ThreadLocal<CompilerPass> current = new ThreadLocal<>();
	
	public static void setCurrent(CompilerPass pass) {
		current.set(pass);
	}
	
	public boolean active() {
		return current.get() == this;
	}
	
	public boolean inactive() {
		return current.get() != this;
	}
}
