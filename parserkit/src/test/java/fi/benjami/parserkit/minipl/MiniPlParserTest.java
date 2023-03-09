package fi.benjami.parserkit.minipl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import fi.benjami.parserkit.lexer.Lexer;
import fi.benjami.parserkit.lexer.TokenTransformer;
import fi.benjami.parserkit.lexer.TokenizedText;
import fi.benjami.parserkit.parser.Parser;
import fi.benjami.parserkit.parser.ast.AstNode;
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
	
	private AstNode parse(Class<? extends AstNode> type, String text) {
		return parser.parse(type, tokenize(text));
	}
	
	@BeforeAll
	public void init() throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException, IOException {
		parser = Parser.compileAndLoad(MiniPlNodes.REGISTRY, MiniPlTokenType.values());
		System.out.println(Parser.compile("foo", MiniPlNodes.REGISTRY, MiniPlTokenType.values()).length / 1024);
//		parser.getClass().getField("HOOK").set(null, new TestParserHook());
	}
	
	@Test
	public void values() {
		assertEquals(new Constant(10), parse(MiniPlNodes.Constant.class, "10"));
		// TODO string constant lexing
	}
	
	@Test
	public void simpleExpressions() {
		assertEquals(new Literal(new Constant(5)),
				parse(MiniPlNodes.Literal.class, "5"));
		
		assertEquals(new AddExpr(new Literal(new Constant(5)), new Literal(new Constant(3))),
				parse(AddExpr.class, "5 + 3"));
		assertEquals(new SubtractExpr(new Literal(new Constant(5)), new Literal(new Constant(3))),
				parse(SubtractExpr.class, "5 - 3"));
		assertEquals(new MultiplyExpr(new Literal(new Constant(5)), new Literal(new Constant(3))),
				parse(MultiplyExpr.class, "5 * 3"));
		assertEquals(new DivideExpr(new Literal(new Constant(5)), new Literal(new Constant(3))),
				parse(DivideExpr.class, "5 / 3"));
		
		assertEquals(new LogicalNotExpr(new Literal(new Constant(5))),
				parse(LogicalNotExpr.class, "!5"));
		assertEquals(new LogicalAndExpr(new Literal(new Constant(5)), new Literal(new Constant(3))),
				parse(LogicalAndExpr.class, "5 & 3"));
		
		assertEquals(new EqualsExpr(new Literal(new Constant(5)), new Literal(new Constant(3))),
				parse(EqualsExpr.class, "5 = 3"));
		assertEquals(new LessThanExpr(new Literal(new Constant(5)), new Literal(new Constant(3))),
				parse(LessThanExpr.class, "5 < 3"));
	}
	
	@Test
	public void arithmeticOrder() {
		assertEquals(new AddExpr(
				new Literal(new Constant(2)),
				new AddExpr(new Literal(new Constant(3)), new Literal(new Constant(5)))
		), parse(AddExpr.class, "2 + 3 + 5"));
		assertEquals(new AddExpr(
				new MultiplyExpr(new Literal(new Constant(2)), new Literal(new Constant(3))),
				new Literal(new Constant(5))
				), parse(AddExpr.class, "2 * 3 + 5"));
		assertEquals(new AddExpr(
				new MultiplyExpr(new Literal(new Constant(2)), new Literal(new Constant(3))),
				new DivideExpr(new Literal(new Constant(5)), new Literal(new Constant(1)))
				), parse(AddExpr.class, "2 * 3 + 5 / 1"));
	}
	
	@Test
	public void block() {
		assertEquals(new Block(List.of(
				new BuiltinRead("a"),
				new BuiltinPrint(new Literal(new VarReference("b"))))
				), parse(Block.class, "read a; print b;"));
	}
	
	@Test
	public void ifBlock() {
		assertEquals(new IfBlock(
				new EqualsExpr(new Literal(new Constant(1)), new Literal(new VarReference("b"))),
				new Block(List.of(new BuiltinPrint(new Literal(new VarReference("a"))))),
				new Block(List.of(new BuiltinPrint(new Literal(new VarReference("b")))))
				), parse(IfBlock.class, "if 1 = b do print a; else print b; end if;"));
	}
}
