package fi.benjami.parserkit.parser.ast;

public interface AstNode {

	static final AstNode MISSING = new Missing();
	
	class Missing implements AstNode {
		
	}
}
