package fi.benjami.code4jvm.lua.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import fi.benjami.code4jvm.lua.parser.LuaLexer;
import fi.benjami.code4jvm.lua.parser.LuaToken;
import fi.benjami.code4jvm.lua.parser.LuaTokenTransformer;
import fi.benjami.parserkit.lexer.Lexer;
import fi.benjami.parserkit.lexer.Token;
import fi.benjami.parserkit.lexer.TokenTransformer;
import fi.benjami.parserkit.lexer.TokenizedText;

public class LexerTest {

	private final Lexer lexer = new LuaLexer();
	private final TokenTransformer transformer = new LuaTokenTransformer();
	
	private TokenizedText.View tokenize(String text) {
		var tokenized = new TokenizedText(lexer, transformer);
		return tokenized.apply(text, 0, 0);
	}
	
	private void assertViewEquals(TokenizedText.View view, Token... expected) {
		for (int i = 0; i < expected.length; i++) {
			assertTrue(view.hasNext());
			assertEquals(expected[i], view.pop());
		}
		assertFalse(view.hasNext());
	}
	
	@Test
	public void helloWorld() {
		assertViewEquals(tokenize("print('Hello world!')"),
				LuaToken.NAME.read(0, "print"),
				LuaToken.GROUP_BEGIN.read(5, "("),
				LuaToken.STRING_LITERAL.read(6, "Hello world!", 14),
				LuaToken.GROUP_END.read(20, ")")
				);
	}
	
	@Test
	public void nestedMath() {
		assertViewEquals(tokenize("2 * 3 + 10 // 5 + 3 ^ 4"),
				LuaToken.LITERAL_NUMBER.read(0, "2"),
				LuaToken.MULTIPLY.read(2, "*"),
				LuaToken.LITERAL_NUMBER.read(4, "3"),
				LuaToken.ADD.read(6, "+"),
				LuaToken.LITERAL_NUMBER.read(8, "10"),
				LuaToken.FLOOR_DIVIDE.read(11, "//"),
				LuaToken.LITERAL_NUMBER.read(14, "5"),
				LuaToken.ADD.read(16, "+"),
				LuaToken.LITERAL_NUMBER.read(18, "3"),
				LuaToken.POWER.read(20, "^"),
				LuaToken.LITERAL_NUMBER.read(22, "4")
				);
	}
}
