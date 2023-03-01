package fi.benjami.parserkit.minipl;

import fi.benjami.parserkit.lexer.Token;
import fi.benjami.parserkit.lexer.TokenTransformer;

public class MiniPlTransformer implements TokenTransformer {

	private static final int IDENTIFIER = MiniPlTokenType.IDENTIFIER.ordinal();
	
	@Override
	public Token transform(Token input) {
		if (input.type() == IDENTIFIER) {
			return switch ((String) input.value() ) {
			case "var" -> MiniPlTokenType.DECLARE_VAR.convert(input, null);
			case "for" -> MiniPlTokenType.FOR.convert(input, null);
			case "in" -> MiniPlTokenType.FOR_IN.convert(input, null);
			case "if" -> MiniPlTokenType.IF.convert(input, null);
			case "else" -> MiniPlTokenType.IF_ELSE.convert(input, null);
			case "do" -> MiniPlTokenType.BLOCK_DO.convert(input, null);
			case "end" -> MiniPlTokenType.BLOCK_END.convert(input, null);
			case "read" -> MiniPlTokenType.BUILTIN_READ.convert(input, null);
			case "print" -> MiniPlTokenType.BUILTIN_PRINT.convert(input, null);
			default -> input;
			};
		} else {
			return input;
		}
	}

}
