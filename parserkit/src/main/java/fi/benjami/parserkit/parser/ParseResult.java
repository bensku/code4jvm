package fi.benjami.parserkit.parser;

import java.util.Set;

import fi.benjami.parserkit.parser.ast.AstNode;

public record ParseResult<T extends AstNode>(
		T node,
		Set<ParseError> errors
) {

}
