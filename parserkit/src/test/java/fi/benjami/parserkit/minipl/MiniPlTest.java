package fi.benjami.parserkit.minipl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import fi.benjami.parserkit.lexer.Token;
import fi.benjami.parserkit.lexer.TokenizedText;
import fi.benjami.parserkit.minipl.token.ArithmeticToken;
import fi.benjami.parserkit.minipl.token.ArithmeticToken.Op;
import fi.benjami.parserkit.minipl.token.AssignmentToken;
import fi.benjami.parserkit.minipl.token.IdentifierToken;
import fi.benjami.parserkit.minipl.token.IntLiteralToken;
import fi.benjami.parserkit.minipl.token.StatementEndToken;
import fi.benjami.parserkit.minipl.token.VarTypeToken;

public class MiniPlTest {

	@Test
	public void sample1() {
		var code = """
					var X : int := 4 + (6 * 2);
					print X;
					""";
		var text = new TokenizedText(new HandWrittenLexer());
		var view = text.apply(code, 0, 0);
		var expected = new Token[] {
				new IdentifierToken(0, "var"), new IdentifierToken(4, "X"), new VarTypeToken(6),
				new IdentifierToken(8, "int"), new AssignmentToken(12),
				new IntLiteralToken(15, "4"), new ArithmeticToken(17, 1, Op.ADD),
				new ArithmeticToken(19, 1, Op.GROUP_BEGIN), new IntLiteralToken(20, "6"),
				new ArithmeticToken(22, 1, Op.MULTIPLY), new IntLiteralToken(24, "2"),
				new ArithmeticToken(25, 1, Op.GROUP_END), new StatementEndToken(26),
				new IdentifierToken(28, "print"), new IdentifierToken(34, "X"),
				new StatementEndToken(35)
		};
		for (int i = 0; i < expected.length; i++) {
			assertTrue(view.hasNext());
			assertEquals(expected[i], view.pop());
		}
		assertFalse(view.hasNext());
		
		
		var expected2 = new Token[] {
				new IdentifierToken(0, "var"), new IdentifierToken(4, "X"), new VarTypeToken(6),
				new IdentifierToken(8, "int"), new AssignmentToken(12),
				new IntLiteralToken(15, "4"), new ArithmeticToken(16, 1, Op.MULTIPLY),
				new IntLiteralToken(18, "2"),
				new ArithmeticToken(19, 1, Op.GROUP_END), new StatementEndToken(20),
				new IdentifierToken(22, "print"), new IdentifierToken(28, "X"),
				new StatementEndToken(29)
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
				print " : Hello, World!\n";
				end for;
				if x = ntimes do
				print “x is equal to ntimes);
				end if;
				""";
		var text = new TokenizedText(new HandWrittenLexer());
		var view = text.apply(code, 0, 0);
//		while (view.hasNext()) {
//			System.out.println(view.pop());
//		}
	}
}
