package fi.benjami.parserkit.minipl.compiler;

import java.util.Map;

import fi.benjami.code4jvm.Type;
import fi.benjami.parserkit.minipl.parser.MiniPlNodes.Expression;

public record TypeTable(
		Map<String, Type> varTypes,
		Map<Expression, Type> exprTypes		
) {}
