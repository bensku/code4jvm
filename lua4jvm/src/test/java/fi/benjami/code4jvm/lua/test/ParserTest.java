package fi.benjami.code4jvm.lua.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import fi.benjami.code4jvm.lua.parser.BinaryExpr;
import fi.benjami.code4jvm.lua.parser.Expression;
import fi.benjami.code4jvm.lua.parser.Expression.VarReference;
import fi.benjami.code4jvm.lua.parser.LuaLexer;
import fi.benjami.code4jvm.lua.parser.LuaNode;
import fi.benjami.code4jvm.lua.parser.LuaToken;
import fi.benjami.code4jvm.lua.parser.LuaTokenTransformer;
import fi.benjami.code4jvm.lua.parser.SpecialNodes;
import fi.benjami.code4jvm.lua.parser.Statement;
import fi.benjami.parserkit.lexer.Lexer;
import fi.benjami.parserkit.lexer.TokenTransformer;
import fi.benjami.parserkit.lexer.TokenizedText;
import fi.benjami.parserkit.parser.Parser;
import fi.benjami.parserkit.parser.ast.AstNode;

@TestInstance(Lifecycle.PER_CLASS)
public class ParserTest {

	private final Lexer lexer = new LuaLexer();
	private final TokenTransformer transformer = new LuaTokenTransformer();
	
	private Parser parser;
	
	private TokenizedText.View tokenize(String text) {
		var tokenized = new TokenizedText(lexer, transformer);
		return tokenized.apply(text, 0, 0);
	}
	
	private <T extends AstNode> T parse(Class<T> type, String text) {
		var result = parser.parseFully(type, tokenize(text));
		assertEquals(Set.of(), result.errors());
		return result.node();
	}
	
	@BeforeAll
	public void init() throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException, IOException {
//		DebugOptions.PRINT_METHODS = true;
		parser = Parser.compileAndLoad(LuaNode.REGISTRY, LuaToken.values());
//		Files.write(Path.of("Debug.class"), Parser.compile("foo", LuaNode.REGISTRY, LuaToken.values()));
		System.out.println(Parser.compile("foo", LuaNode.REGISTRY, LuaToken.values()).length / 1024);
//		parser.getClass().getField("HOOK").set(null, new TestParserHook());
	}
	
	@Test
	public void basicMath() {
		assertEquals(new BinaryExpr.Add(
				new Expression.SimpleConstant(20.0), new Expression.SimpleConstant(1.0)),
				parse(BinaryExpr.Add.class, "20 + 1"));
		assertEquals(new BinaryExpr.Subtract(
				new Expression.SimpleConstant(21.0), new Expression.SimpleConstant(1.0)),
				parse(BinaryExpr.Subtract.class, "21 - 1"));
		assertEquals(new BinaryExpr.Multiply(
				new Expression.SimpleConstant(22.0), new Expression.SimpleConstant(1.0)),
				parse(BinaryExpr.Multiply.class, "22 * 1"));
		assertEquals(new BinaryExpr.Divide(
				new Expression.SimpleConstant(23.0), new Expression.SimpleConstant(2.0)),
				parse(BinaryExpr.Divide.class, "23 / 2"));
	}
	
	@Test
	public void nestedMath() {
		assertEquals(new BinaryExpr.Add(
				new Expression.SimpleConstant(3.0),
				new BinaryExpr.Multiply(new Expression.SimpleConstant(2.0), new Expression.SimpleConstant(3.0))
				), parse(BinaryExpr.Add.class, "3 + 2 * 3"));
		assertEquals(new BinaryExpr.Add(
				new BinaryExpr.Multiply(new Expression.SimpleConstant(2.0), new Expression.SimpleConstant(3.0)),
				new BinaryExpr.Add(
						new BinaryExpr.FloorDivide(new Expression.SimpleConstant(10.0), new Expression.SimpleConstant(5.0)),
						new BinaryExpr.Power(new Expression.SimpleConstant(3.0), new Expression.SimpleConstant(4.0))
						)
				), parse(BinaryExpr.Add.class, "2 * 3 + 10 // 5 + 3 ^ 4"));
		assertEquals(new BinaryExpr.Add(
				new Expression.Group(new BinaryExpr.Add(
						new Expression.SimpleConstant(2.0),
						new Expression.SimpleConstant(3.0)
						)),
				new Expression.SimpleConstant(1.0)
				), parse(BinaryExpr.Add.class, "(2 + 3) + 1"));
		assertEquals(new BinaryExpr.Add(
				new Expression.SimpleConstant(1.0),
				new Expression.Group(new BinaryExpr.Add(
						new Expression.SimpleConstant(2.0),
						new Expression.SimpleConstant(3.0)
						))
				), parse(BinaryExpr.Add.class, "1 + (2 + 3)"));
		assertEquals(new BinaryExpr.Add(
				new BinaryExpr.Multiply(
						new Expression.SimpleConstant(2.0),
						new BinaryExpr.FloorDivide(
								new Expression.Group(new BinaryExpr.Add(
										new Expression.SimpleConstant(3.0),
										new Expression.SimpleConstant(10.0)
										)),
								new Expression.SimpleConstant(5.0)
								)
						),
				new BinaryExpr.Power(
						new Expression.SimpleConstant(3.0),
						new Expression.SimpleConstant(4.0)
						)
				), parse(BinaryExpr.Add.class, "2 * (3 + 10) // 5 + 3 ^ 4"));
	}
	
	@Test
	public void variableAssignment() {
		assertEquals(new Statement.VarAssignment(
				List.of(new Expression.VarReference(List.of("a"), null)),
				List.of(new Expression.VarReference(List.of("b"), null))
				), parse(Statement.VarAssignment.class, "a = b"));
		assertEquals(new Statement.VarAssignment(
				List.of(new Expression.VarReference(List.of("a"), null), new Expression.VarReference(List.of("b"), null)),
				List.of(new Expression.VarReference(List.of("c"), null), new Expression.VarReference(List.of("d"), null))
				), parse(Statement.VarAssignment.class, "a, b = c, d"));
		assertEquals(new Statement.VarAssignment(
				List.of(new Expression.VarReference(List.of("a", "b"), null), new Expression.VarReference(List.of("c", "d", "e"), null)),
				List.of(new Expression.VarReference(List.of("f"), null), new Expression.VarReference(List.of("g", "h"), null))
				), parse(Statement.VarAssignment.class, "a.b, c.d.e = f, g.h"));
	}
	
	@Test
	public void arrayAccess() {
		assertEquals(new Statement.VarAssignment(
				List.of(
						new Expression.VarReference(List.of("a"), null),
						new Expression.VarReference(List.of("b"), new Expression.VarReference(List.of("c"), null))
						),
				List.of(
						new Expression.VarReference(List.of("d"), new Expression.VarReference(List.of("e"), null)),
						new Expression.VarReference(List.of("f"), null)
						)
				), parse(Statement.VarAssignment.class, "a, b[c] = d[e], f"));
	}
	
	@Test
	public void localVariables() {
		assertEquals(new Statement.LocalVarDeclaration(
				List.of("a"),
				List.of(new Expression.VarReference(List.of("b"), null))
				), parse(Statement.LocalVarDeclaration.class, "local a = b"));
		assertEquals(new Statement.LocalVarDeclaration(
				List.of("a", "b"),
				List.of(new Expression.VarReference(List.of("c"), null), new Expression.VarReference(List.of("d"), null))
				), parse(Statement.LocalVarDeclaration.class, "local a, b = c, d"));
	}
	
	@Test
	public void emptyChunk() {
		parse(SpecialNodes.Chunk.class, "");
	}
	
	@Test
	public void functionCalls() {
		assertEquals(new Expression.FunctionCall(
				new Expression.VarReference(List.of("f"), null),
				null,
				List.of()
				), parse(Expression.FunctionCall.class, "f()"));
		assertEquals(new Expression.FunctionCall(
				new Expression.VarReference(List.of("f"), null),
				null,
				List.of(
						new Expression.VarReference(List.of("a"), null),
						new Expression.VarReference(List.of("b"), null)
						)
				), parse(Expression.FunctionCall.class, "f(a, b)"));
		assertEquals(new Expression.FunctionCall(
				new Expression.VarReference(List.of("f"), null),
				null,
				List.of(new Expression.StringConstant("test"))
				), parse(Expression.FunctionCall.class, "f 'test'"));
		assertEquals(new Expression.FunctionCall(
				new Expression.VarReference(List.of("f", "g"), null),
				null,
				List.of(
						new Expression.VarReference(List.of("a"), null),
						new Expression.VarReference(List.of("b"), null)
						)
				), parse(Expression.FunctionCall.class, "f.g(a, b)"));
		assertEquals(new Expression.FunctionCall(
				new Expression.VarReference(List.of("f", "g"), null),
				"h",
				List.of(
						new Expression.VarReference(List.of("a"), null),
						new Expression.VarReference(List.of("b"), null)
						)
				), parse(Expression.FunctionCall.class, "f.g:h(a, b)"));
		
		parse(SpecialNodes.Block.class, "f(1, 2)");
		parse(Statement.DoEndBlock.class, "do f(1, 2) end");
	}
	
	@Test
	public void returnStmt() {
		parse(Statement.Return.class, "return");
		parse(Statement.Return.class, "return 1");
		parse(Statement.Return.class, "return f");
		parse(Statement.Return.class, "return f(1)");
	}
	
	@Test
	public void callInsideFunction() {
		parse(Expression.FunctionDefinition.class, "function (f) return f(1, 2.5) end");
	}
	
	@Test
	public void tableConstructor() {
		assertEquals(new Expression.TableConstructor(List.of()),
				parse(Expression.TableConstructor.class, "{}"));
		assertEquals(new Expression.TableConstructor(List.of(
				new Expression.TableField(null, null, new Expression.SimpleConstant(1d)),
				new Expression.TableField(null, null, new Expression.SimpleConstant(2d)),
				new Expression.TableField(null, null, new Expression.SimpleConstant(3d))
				)),
				parse(Expression.TableConstructor.class, "{1, 2, 3}"));
		assertEquals(new Expression.TableConstructor(List.of(
				new Expression.TableField("a", null, new Expression.SimpleConstant(1d)),
				new Expression.TableField("b", null, new Expression.SimpleConstant(2d)),
				new Expression.TableField(null, null, new Expression.SimpleConstant(3d))
				)),
				parse(Expression.TableConstructor.class, "{a = 1, b = 2, 3}"));
		assertEquals(new Expression.TableConstructor(List.of(
				new Expression.TableField("a", null, new Expression.SimpleConstant(1d)),
				new Expression.TableField(null, new Expression.StringConstant("b"), new Expression.SimpleConstant(2d)),
				new Expression.TableField(null, null, new Expression.SimpleConstant(3d))
				)),
				parse(Expression.TableConstructor.class, "{a = 1, ['b'] = 2, 3}"));
		assertEquals(new Expression.TableConstructor(List.of(
				new Expression.TableField("a", null, new Expression.SimpleConstant(1d)),
				new Expression.TableField(null, new Expression.StringConstant("b"), new Expression.SimpleConstant(2d)),
				new Expression.TableField(
						null,
						new Expression.FunctionCall(
								new Expression.VarReference(List.of("f"), null),
								null,
								List.of(new Expression.StringConstant("c"))
								),
						new Expression.SimpleConstant(3d))
				)),
				parse(Expression.TableConstructor.class, "{a = 1, ['b'] = 2, [f('c')] = 3}"));
	}
	
	@Test
	public void ifBlock() {
		assertEquals(new Statement.IfBlock(
				new VarReference(List.of("a"), null),
				new SpecialNodes.Block(List.of(
						new Statement.Return(List.of(new VarReference(List.of("b"), null)))
						)),
				List.of(),
				null
				),
				parse(Statement.IfBlock.class, "if a then return b end"));
		assertEquals(new Statement.IfBlock(
				new VarReference(List.of("a"), null),
				new SpecialNodes.Block(List.of(
						new Statement.Return(List.of(new VarReference(List.of("b"), null)))
						)),
				List.of(),
				new SpecialNodes.Block(List.of(
						new Statement.Return(List.of(new VarReference(List.of("c"), null)))
						))
				),
				parse(Statement.IfBlock.class, "if a then return b else return c end"));
		assertEquals(new Statement.IfBlock(
				new VarReference(List.of("a"), null),
				new SpecialNodes.Block(List.of(
						new Statement.Return(List.of(new VarReference(List.of("b"), null)))
						)),
				List.of(new Statement.ElseIfClause(
						new VarReference(List.of("c"), null),
						new SpecialNodes.Block(List.of(
								new Statement.Return(List.of(new VarReference(List.of("d"), null)))
								))
						)),
				new SpecialNodes.Block(List.of(
						new Statement.Return(List.of(new VarReference(List.of("e"), null)))
						))
				),
				parse(Statement.IfBlock.class, "if a then return b elseif c then return d else return e end"));
		assertEquals(new Statement.IfBlock(
				new BinaryExpr.Equal(new VarReference(List.of("a"), null), new Expression.SimpleConstant(3d)),
				new SpecialNodes.Block(List.of(
						new Statement.Return(List.of(new VarReference(List.of("b"), null)))
						)),
				List.of(new Statement.ElseIfClause(
						new BinaryExpr.NotEqual(new Expression.SimpleConstant(5d), new VarReference(List.of("c"), null)),
						new SpecialNodes.Block(List.of(
								new Statement.Return(List.of(new VarReference(List.of("d"), null)))
								))
						)),
				new SpecialNodes.Block(List.of(
						new Statement.Return(List.of(new VarReference(List.of("e"), null)))
						))
				),
				parse(Statement.IfBlock.class, "if a == 3 then return b elseif 5 ~= c then return d else return e end"));
	}
	
	@Test
	public void logicalExpressions() {
		assertEquals(new SpecialNodes.Block(List.of(new Statement.Return(List.of(new BinaryExpr.LogicalAnd(
				new BinaryExpr.Equal(new VarReference(List.of("a"), null), new VarReference(List.of("b"), null)),
				new BinaryExpr.Equal(new VarReference(List.of("b"), null), new VarReference(List.of("c"), null))
				))))), parse(SpecialNodes.Block.class, "return a == b and b == c"));
	}
	
	@Test
	public void functionDeclarations() {
		parse(SpecialNodes.Block.class, "function f(x) end");
		parse(SpecialNodes.Block.class, "local function f(x) end");
	}
}
