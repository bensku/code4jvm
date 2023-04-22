package fi.benjami.code4jvm.lua.parser;

import fi.benjami.parserkit.lexer.Token;
import fi.benjami.parserkit.lexer.TokenTransformer;

public class LuaTokenTransformer implements TokenTransformer {

	private static final int NAME = LuaToken.NAME.ordinal();
	
	@Override
	public Token transform(Token input) {
		if (input.type() == NAME) {
			return switch ((String) input.value()) {
			case "break" -> LuaToken.LOOP_BREAK.convert(input);
			case "goto" -> LuaToken.GOTO_JUMP.convert(input);
			case "do" -> LuaToken.DO.convert(input);
			case "end" -> LuaToken.END.convert(input);
			case "while" -> LuaToken.WHILE_LOOP.convert(input);
			case "repeat" -> LuaToken.REPEAT_LOOP.convert(input);
			case "until" -> LuaToken.REPEAT_UNTIL.convert(input);
			case "if" -> LuaToken.IF_BLOCK.convert(input);
			case "then" -> LuaToken.IF_THEN.convert(input);
			case "elseif" -> LuaToken.IF_ELSE_IF.convert(input);
			case "else" -> LuaToken.IF_ELSE.convert(input);
			case "for" -> LuaToken.FOR_LOOP.convert(input);
			case "in" -> LuaToken.FOR_IN.convert(input);
			case "function" -> LuaToken.FUNCTION.convert(input);
			case "local" -> LuaToken.LOCAL.convert(input);
			case "return" -> LuaToken.RETURN.convert(input);
			case "and" -> LuaToken.LOGICAL_AND.convert(input);
			case "or" -> LuaToken.LOGICAL_OR.convert(input);
			case "not" -> LuaToken.LOGICAL_NOT.convert(input);
			case "nil" -> LuaToken.LITERAL_NIL.convert(input);
			case "false" -> LuaToken.LITERAL_FALSE.convert(input);
			case "true" -> LuaToken.LITERAL_TRUE.convert(input);
			default -> input;
			};
		} else {
			return input;
		}
	}

}
