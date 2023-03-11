package fi.benjami.parserkit.minipl.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import fi.benjami.parserkit.lexer.Lexer;
import fi.benjami.parserkit.lexer.TokenTransformer;
import fi.benjami.parserkit.lexer.TokenizedText;
import fi.benjami.parserkit.parser.CompileError;
import fi.benjami.parserkit.parser.Parser;
import fi.benjami.parserkit.parser.ast.AstNode;
import fi.benjami.parserkit.minipl.HandWrittenLexer;
import fi.benjami.parserkit.minipl.MiniPlError;
import fi.benjami.parserkit.minipl.MiniPlNodes;
import fi.benjami.parserkit.minipl.MiniPlNodes.*;
import fi.benjami.parserkit.minipl.MiniPlTokenType;
import fi.benjami.parserkit.minipl.MiniPlTransformer;

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
		var result = parser.parse(type, tokenize(text));
		assertEquals(Set.of(), result.errors());
		return result.node();
	}
	
	private AstNode parseProgram(String text) {
		var result = parser.parseFully(Program.class, tokenize(text));
		assertEquals(Set.of(), result.errors());
		return result.node();
	}
	
	@BeforeAll
	public void init() throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException, IOException {
//		DebugOptions.PRINT_METHODS = true;
		parser = Parser.compileAndLoad(MiniPlNodes.REGISTRY, MiniPlTokenType.values());
		System.out.println(Parser.compile("foo", MiniPlNodes.REGISTRY, MiniPlTokenType.values()).length / 1024);
//		parser.getClass().getField("HOOK").set(null, new TestParserHook());
	}
	
	@Test
	public void values() {
		assertEquals(new Constant(10), parse(Constant.class, "10"));
		assertEquals(new Constant("foo"), parse(Constant.class, "\"foo\""));
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
	
	@Test
	public void forBlock() {
		var text = """
				for x in 0..nTimes-1 do
				print x;
				print " : Hello, World!\\n";
				end for;
				""";
		var node = parse(ForBlock.class, text);
		assertEquals(new ForBlock(
						"x",
						new Literal(new Constant(0)),
						new SubtractExpr(
								new Literal(new VarReference("nTimes")),
								new Literal(new Constant(1))
								),
						new Block(List.of(
								new BuiltinPrint(new Literal(new VarReference("x"))),
								new BuiltinPrint(new Literal(new Constant(" : Hello, World!\\n")))
								))
						), node);
	}
	
	@Test
	public void forBlockInProgram() {
		var text = """
				for x in 0..nTimes-1 do
				print x;
				print " : Hello, World!\\n";
				end for;
				""";
		var node = parse(Program.class, text);
		assertEquals(new Program(new Block(List.of(new ForBlock(
						"x",
						new Literal(new Constant(0)),
						new SubtractExpr(
								new Literal(new VarReference("nTimes")),
								new Literal(new Constant(1))
								),
						new Block(List.of(
								new BuiltinPrint(new Literal(new VarReference("x"))),
								new BuiltinPrint(new Literal(new Constant(" : Hello, World!\\n")))
								))
						)))), node);
	}
	
	@Test
	public void ifBlock2() {
		var text = """
				if x = nTimes do
				print "x is equal to ntimes";
				end if;
				""";
		var node = parse(IfBlock.class, text);
		assertEquals(new IfBlock(
						new EqualsExpr(
								new Literal(new VarReference("x")),
								new Literal(new VarReference("nTimes"))
								),
						new Block(List.of(
								new BuiltinPrint(new Literal(new Constant("x is equal to ntimes")))
								)),
						null
						), node);
	}
	
	@Test
	public void errorToken() {
		var result = parser.parse(AddExpr.class, tokenize("{"));
		assertNull(result.node());
		assertEquals(Set.of(
				new CompileError(MiniPlError.MISSING_EXPRESSION, 1, 1),
				new CompileError(CompileError.LEXICAL, 0, 1)
				), result.errors());
	}
	
	@Test
	public void missingExprs() {
		// Basic expression
		var result = parser.parse(SubtractExpr.class, tokenize("a - "));
		assertEquals(new SubtractExpr(
				new Literal(new VarReference("a")),
				null
				), result.node());
		assertEquals(Set.of(new CompileError(MiniPlError.MISSING_EXPRESSION, 3, 3)), result.errors());
		
		// A bit more complicated error recovery
		// Note: the odd parse order is because we specially request parsing MultiplyExpr
		var result2 = parser.parse(MultiplyExpr.class, tokenize("a * c + "));
		assertEquals(new MultiplyExpr(
				new Literal(new VarReference("a")),
				new AddExpr(
						new Literal(new VarReference("c")),
						null
						)
				), result2.node());
		assertEquals(Set.of(new CompileError(MiniPlError.MISSING_EXPRESSION, 7, 7)), result2.errors());
	}
	
	@Test
	public void varDeclarations() {
		var node1 = parse(VarDeclaration.class, "var x : int;");
		assertEquals(new VarDeclaration("x", "int", null), node1);
		
		var node2 = parse(VarDeclaration.class, "var x : int := 1;");
		assertEquals(new VarDeclaration("x", "int", new Literal(new Constant(1))), node2);
	}
	
	@Test
	public void missingSemicolon() {
		// Automatic semicolon insertion!
		var result = parser.parse(Block.class, tokenize("read a print b print c"));
		assertEquals(new Block(List.of(
				new BuiltinRead("a"),
				new BuiltinPrint(new Literal(new VarReference("b"))),
				new BuiltinPrint(new Literal(new VarReference("c")))
				)), result.node());
		assertEquals(Set.of(
				new CompileError(MiniPlError.MISSING_SEMICOLON, 6, 6),
				new CompileError(MiniPlError.MISSING_SEMICOLON, 14, 14),
				new CompileError(MiniPlError.MISSING_SEMICOLON, 22, 22)
				), result.errors());
	}
	
	@Test
	public void partialStatement() {
		var result = parser.parse(BuiltinPrint.class, tokenize("print a + b *"));
		assertEquals(new BuiltinPrint(
				new AddExpr(
						new Literal(new VarReference("a")),
						new MultiplyExpr(
								new Literal(new VarReference("b")),
								null
								)
						)
				), result.node());
		assertEquals(Set.of(
				new CompileError(MiniPlError.MISSING_EXPRESSION, 13, 13),
				new CompileError(MiniPlError.MISSING_SEMICOLON, 13, 13)
				), result.errors());
		
		// Something more challenging that we can't recover from
		// (but semicolon addition should still work)
		var result2 = parser.parse(BuiltinPrint.class, tokenize("print + b *"));
		assertEquals(new BuiltinPrint(null), result2.node());
		assertEquals(Set.of(
				new CompileError(MiniPlError.MISSING_EXPRESSION, 5, 5),
				new CompileError(MiniPlError.MISSING_SEMICOLON, 5, 5)
				), result2.errors());
	}
	
	@Test
	public void sample1() {
		var src = """
				var X : int := 4 + (6 * 2);
				print X;
				""";
		var node = parseProgram(src);
		assertEquals(new Program(new Block(List.of(
				new VarDeclaration("X", "int", new AddExpr(
						new Literal(new Constant(4)),
						new Group(new MultiplyExpr(
								new Literal(new Constant(6)),
								new Literal(new Constant(2))
								))
						)),
				new BuiltinPrint(new Literal(new VarReference("X")))
				))), node);
	}
	
	@Test
	public void sample2() {
		var text = """
				var nTimes : int := 0;
				print "How many times?";
				read nTimes;
				var x : int;
				for x in 0..nTimes-1 do
				print x;
				print " : Hello, World!\\n";
				end for;
				if x = nTimes do
				print "x is equal to ntimes";
				end if;
				""";
		var node = parseProgram(text);
		assertEquals(new Program(new Block(List.of(
				new VarDeclaration("nTimes", "int", new Literal(new Constant(0))),
				new BuiltinPrint(new Literal(new Constant("How many times?"))),
				new BuiltinRead("nTimes"),
				new VarDeclaration("x", "int", null),
				new ForBlock(
						"x",
						new Literal(new Constant(0)),
						new SubtractExpr(
								new Literal(new VarReference("nTimes")),
								new Literal(new Constant(1))
								),
						new Block(List.of(
								new BuiltinPrint(new Literal(new VarReference("x"))),
								new BuiltinPrint(new Literal(new Constant(" : Hello, World!\\n")))
								))
						),
				new IfBlock(
						new EqualsExpr(
								new Literal(new VarReference("x")),
								new Literal(new VarReference("nTimes"))
								),
						new Block(List.of(
								new BuiltinPrint(new Literal(new Constant("x is equal to ntimes")))
								)),
						null
						)
				))), node);
	}
}
