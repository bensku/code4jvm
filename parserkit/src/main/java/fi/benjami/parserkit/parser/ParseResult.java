package fi.benjami.parserkit.parser;

import java.util.Set;

import fi.benjami.parserkit.parser.ast.AstNode;

public record ParseResult(
		AstNode node,
		Set<CompileError> errors
) {

}
