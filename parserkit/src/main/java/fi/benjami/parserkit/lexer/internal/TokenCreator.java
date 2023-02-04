package fi.benjami.parserkit.lexer.internal;

import fi.benjami.code4jvm.call.CallTarget;

public record TokenCreator(
		CallTarget method,
		boolean needsContent
) {

}
