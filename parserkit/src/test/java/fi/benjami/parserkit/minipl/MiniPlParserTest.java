package fi.benjami.parserkit.minipl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import fi.benjami.parserkit.lexer.Lexer;
import fi.benjami.parserkit.lexer.TokenTransformer;
import fi.benjami.parserkit.lexer.TokenizedText;
import fi.benjami.parserkit.parser.Parser;
import fi.benjami.parserkit.parser.ParserFlag;
import fi.benjami.parserkit.minipl.MiniPlNodes.*;

@TestInstance(Lifecycle.PER_CLASS)
public class MiniPlParserTest {

	private final Lexer lexer = new HandWrittenLexer();
	private final TokenTransformer transformer = new MiniPlTransformer();
	private Parser parser;
	
	private TokenizedText.View tokenize(String text) {
		var tokenized = new TokenizedText(lexer, transformer);
		return tokenized.apply(text, 0, 0);
	}
	
	@BeforeAll
	public void init() throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
		parser = Parser.compileAndLoad(MiniPlNodes.REGISTRY, MiniPlTokenType.values());
//		parser.getClass().getField("HOOK").set(null, new TestParserHook());
	}
	
	@Test
	public void values() {
		assertEquals(new Constant(10), parser.parse(MiniPlNodes.Constant.class, tokenize("10")));
		// TODO string constant lexing
	}
	
	@Test
	public void simpleExpressions() {
		assertEquals(new Literal(new Constant(5)),
				parser.parse(MiniPlNodes.Literal.class, tokenize("5")));
		
		assertEquals(new AddExpr(new Literal(new Constant(5)), new Literal(new Constant(3))),
				parser.parse(AddExpr.class, tokenize("5 + 3")));
		assertEquals(new SubtractExpr(new Literal(new Constant(5)), new Literal(new Constant(3))),
				parser.parse(SubtractExpr.class, tokenize("5 - 3")));
		assertEquals(new MultiplyExpr(new Literal(new Constant(5)), new Literal(new Constant(3))),
				parser.parse(MultiplyExpr.class, tokenize("5 * 3")));
		assertEquals(new DivideExpr(new Literal(new Constant(5)), new Literal(new Constant(3))),
				parser.parse(DivideExpr.class, tokenize("5 / 3")));
		
		assertEquals(new LogicalNotExpr(new Literal(new Constant(5))),
				parser.parse(LogicalNotExpr.class, tokenize("!5")));
		assertEquals(new LogicalAndExpr(new Literal(new Constant(5)), new Literal(new Constant(3))),
				parser.parse(LogicalAndExpr.class, tokenize("5 & 3")));
		
		assertEquals(new EqualsExpr(new Literal(new Constant(5)), new Literal(new Constant(3))),
				parser.parse(EqualsExpr.class, tokenize("5 = 3")));
		assertEquals(new LessThanExpr(new Literal(new Constant(5)), new Literal(new Constant(3))),
				parser.parse(LessThanExpr.class, tokenize("5 < 3")));
	}
	
	@Test
	public void arithmeticOrder() {
		assertEquals(new AddExpr(
				new Literal(new Constant(2)),
				new AddExpr(new Literal(new Constant(3)), new Literal(new Constant(5)))
		), parser.parse(AddExpr.class, tokenize("2 + 3 + 5")));
		assertEquals(new AddExpr(
				new MultiplyExpr(new Literal(new Constant(2)), new Literal(new Constant(3))),
				new Literal(new Constant(5))
				), parser.parse(AddExpr.class, tokenize("2 * 3 + 5")));
	}
}
