package fi.benjami.parserkit.minipl.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import fi.benjami.parserkit.lexer.Token;
import fi.benjami.parserkit.lexer.TokenizedText;
import fi.benjami.parserkit.minipl.parser.HandWrittenLexer;
import fi.benjami.parserkit.minipl.parser.MiniPlTokenType;
import fi.benjami.parserkit.minipl.parser.MiniPlTransformer;

public class MiniPlLexerTest {

	@Test
	public void sample1() {
		var code = """
					var X : int := 4 + (6 * 2);
					print X;
					""";
		var text = new TokenizedText(new HandWrittenLexer(), new MiniPlTransformer());
		var view = text.apply(code, 0, 0);
		var expected = new Token[] {
				MiniPlTokenType.DECLARE_VAR.read(0, "var"),
				MiniPlTokenType.IDENTIFIER.read(4, "X"),
				MiniPlTokenType.VAR_TYPE.read(6, ":"),
				MiniPlTokenType.IDENTIFIER.read(8, "int"),
				MiniPlTokenType.ASSIGNMENT.read(12, ":="),
				MiniPlTokenType.INT_LITERAL.read(15, "4"),
				MiniPlTokenType.ADD.read(17, "+"),
				MiniPlTokenType.GROUP_BEGIN.read(19, "("),
				MiniPlTokenType.INT_LITERAL.read(20, "6"),
				MiniPlTokenType.MULTIPLY.read(22, "*"),
				MiniPlTokenType.INT_LITERAL.read(24, "2"),
				MiniPlTokenType.GROUP_END.read(25, ")"),
				MiniPlTokenType.STATEMENT_END.read(26, ";"),
				MiniPlTokenType.BUILTIN_PRINT.read(28, "print"),
				MiniPlTokenType.IDENTIFIER.read(34, "X"),
				MiniPlTokenType.STATEMENT_END.read(35, ";")
		};
		for (int i = 0; i < expected.length; i++) {
			assertTrue(view.hasNext());
			assertEquals(expected[i], view.pop());
		}
		assertFalse(view.hasNext());
		
		
		var expected2 = new Token[] {
				MiniPlTokenType.DECLARE_VAR.read(0, "var"),
				MiniPlTokenType.IDENTIFIER.read(4, "X"),
				MiniPlTokenType.VAR_TYPE.read(6, ":"),
				MiniPlTokenType.IDENTIFIER.read(8, "int"),
				MiniPlTokenType.ASSIGNMENT.read(12, ":="),
				MiniPlTokenType.INT_LITERAL.read(15, "4"),
				MiniPlTokenType.MULTIPLY.read(16, "*"),
				MiniPlTokenType.INT_LITERAL.read(18, "2"),
				MiniPlTokenType.GROUP_END.read(19, ")"),
				MiniPlTokenType.STATEMENT_END.read(20, ";"),
				MiniPlTokenType.BUILTIN_PRINT.read(22, "print"),
				MiniPlTokenType.IDENTIFIER.read(28, "X"),
				MiniPlTokenType.STATEMENT_END.read(29, ";"),
		};
		
		text.apply("", 16, 22);
		var view2 = text.viewFromStart();
		for (int i = 0; i < expected2.length; i++) {
			assertTrue(view2.hasNext());
			assertEquals(expected2[i], view2.pop());
		}
		assertFalse(view2.hasNext());
	}
	
	@Test
	public void sample2() {
		// TODO actually test something
		var code = """
				var nTimes : int := 0;
				print "How many times?";
				read nTimes;
				var x : int;
				for x in 0..nTimes-1 do
				print x;
				print " : Hello, World!\\n";
				end for;
				if x = ntimes do
				print “x is equal to ntimes);
				end if;
				""";
		System.out.println(code);
		var text = new TokenizedText(new HandWrittenLexer(), new MiniPlTransformer());
		var view = text.apply(code, 0, 0);
//		while (view.hasNext()) {
//			System.out.println(view.pop());
//		}
	}
	
	@Test
	public void errorToken() {
		var text  = new TokenizedText(new HandWrittenLexer(), new MiniPlTransformer());
		var view = text.apply("{", 0, 0);
		assertEquals(new Token(0, 1, MiniPlTokenType.ERROR.ordinal(), "{"), view.pop());
	}
}
