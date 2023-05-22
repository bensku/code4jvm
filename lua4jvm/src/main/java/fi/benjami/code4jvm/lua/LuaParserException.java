package fi.benjami.code4jvm.lua;

import java.util.Set;

import fi.benjami.parserkit.parser.ParseError;

public class LuaParserException extends RuntimeException {

	private static final long serialVersionUID = 1L;
	
	private final Set<ParseError> errors;
	
	public LuaParserException(Set<ParseError> errors) {
		super(errors.toString()); // TODO better reporting
		this.errors = errors;
	}
	
	// TODO temp constructor
	public LuaParserException(String error) {
		super(error);
		this.errors = Set.of();
	}
	
	public Set<ParseError> errors() {
		return errors;
	}

}
