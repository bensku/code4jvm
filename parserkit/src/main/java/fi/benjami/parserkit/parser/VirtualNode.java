package fi.benjami.parserkit.parser;

import fi.benjami.parserkit.parser.ast.AstNode;

public class VirtualNode {

	@SafeVarargs
	public static VirtualNode of(Class<? extends AstNode>... types) {
		return new VirtualNode(false, 0, types);
	}
	
	@SafeVarargs
	public static VirtualNode parseOrError(int errorType, Class<? extends AstNode>... types) {
		return new VirtualNode(true, errorType, types);
	}
	
	private final boolean handleErrors;
	private final int errorType;
	private final Class<? extends AstNode>[] types;
	
	private VirtualNode(boolean handleErrors, int errorType, Class<? extends AstNode>[] types) {
		this.handleErrors = handleErrors;
		this.errorType = errorType;
		this.types = types;
	}
	
	public boolean handlesErrors() {
		return handleErrors;
	}
	
	public int errorType() {
		if (!handleErrors) {
			throw new IllegalStateException("this virtual node does not handle errors");
		}
		return errorType;
	}
	
	public Class<? extends AstNode>[] astNodeTypes() {
		return types;
	}
}
