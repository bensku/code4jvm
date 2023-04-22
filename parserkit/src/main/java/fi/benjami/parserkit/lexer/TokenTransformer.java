package fi.benjami.parserkit.lexer;

/**
 * Transforms {@link Token tokens} created by lexer to other tokens before they
 * are added to the {@link TokenizedText}.
 * 
 * <p>A common use case for token transformers would be to convert identifiers
 * that represent reserved words into their own dedicated tokens. Doing this
 * during lexing would be challenging and/or expensive.
 *
 */
@FunctionalInterface
public interface TokenTransformer {
	
	static TokenTransformer NO_OP = input -> input;

	Token transform(Token input);
}
