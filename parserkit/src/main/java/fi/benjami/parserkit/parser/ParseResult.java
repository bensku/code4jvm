package fi.benjami.parserkit.parser;

import java.util.List;

import fi.benjami.parserkit.parser.ast.AstNode;

public record ParseResult(
		AstNode node,
		List<ParseError> errors
) {

}
