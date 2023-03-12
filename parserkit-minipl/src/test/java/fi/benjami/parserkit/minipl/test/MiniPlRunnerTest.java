package fi.benjami.parserkit.minipl.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import fi.benjami.parserkit.minipl.MiniPlRunner;
import fi.benjami.parserkit.minipl.WrappedError;
import fi.benjami.parserkit.minipl.compiler.SemanticError;
import fi.benjami.parserkit.minipl.parser.MiniPlError;
import fi.benjami.parserkit.parser.ParseError;

public class MiniPlRunnerTest {
	
	@Test
	public void sample1() {
		var runner = new MiniPlRunner();
		var src = """
				var X : int := 4 + (6 * 2);
				print X;
				""";
		var prog = runner.parse(src);
		assertEquals(List.of(), runner.errors());
		var types = runner.typeCheck(prog);
		assertEquals(List.of(), runner.errors());
		var code = runner.compile(prog, types.orElseThrow());
		var app = runner.load(code);
		app.run();
	}
	
	@Test
	public void sample2() {
		var runner = new MiniPlRunner();
		var src = """
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
		var prog = runner.parse(src);
		assertEquals(List.of(), runner.errors());
		var types = runner.typeCheck(prog);
		assertEquals(List.of(), runner.errors());
		var code = runner.compile(prog, types.orElseThrow());
		var app = runner.load(code);
//		app.run(); // Don't run, this is interactive program
	}
	
	@Test
	public void sample3() {
		var runner = new MiniPlRunner();
		var src = """
				print "Give a number";
				var n : int;
				read n;
				var v : int := 1;
				var i : int;
				for i in 1..n do
				v := v * i;
				end for;
				print "The result is: ";
				print v;
				""";
		var prog = runner.parse(src);
		assertEquals(List.of(), runner.errors());
		var types = runner.typeCheck(prog);
		assertEquals(List.of(), runner.errors());
		var code = runner.compile(prog, types.orElseThrow());
		var app = runner.load(code);
//		app.run(); // Don't run, this is interactive program
	}
	
	@Test
	public void sample3Error() {
		var runner = new MiniPlRunner();
		var src = """
				print "Give a number";
				var n : integer
				read n;
				var v : int := 1;
				var i : int;
				for i in 1..n do
				v := v * i
				end for;
				print "The result is: ";
				print v;
				""";
		var prog = runner.parse(src);
		assertEquals(List.of(
				new WrappedError(new ParseError(MiniPlError.MISSING_SEMICOLON, 38, 38), 38),
				new WrappedError(new ParseError(MiniPlError.MISSING_SEMICOLON, 105, 105), 105)
				), runner.errors());
		runner.typeCheck(prog);
		assertEquals(List.of(
				new WrappedError(new ParseError(MiniPlError.MISSING_SEMICOLON, 38, 38), 38),
				new WrappedError(new ParseError(MiniPlError.MISSING_SEMICOLON, 105, 105), 105),
				new WrappedError(SemanticError.of(SemanticError.Type.UNKNOWN_TYPE, "n", "integer"), Integer.MAX_VALUE)
				), runner.errors());
		assertThrows(IllegalStateException.class, () -> runner.compile(null, null));
	}
	
	@Test
	public void fibonacci() {
		// NOTE: test output will likely be mangled a bit, since other tests also use stdout
		// Use samples if you care about that
		var runner = new MiniPlRunner();
		var src = """
				print "Computing 20th fibonacci number\n";
				var a : int := 0;
				var b : int := 1;
				var i : int;
				for i in 0..10 do
				var tmp : int := a;
				a := a + b;
				b := tmp;
				end for;
				print "num: ";
				print a;
				""";
		var prog = runner.parse(src);
		assertEquals(List.of(), runner.errors());
		var types = runner.typeCheck(prog);
		assertEquals(List.of(), runner.errors());
		var code = runner.compile(prog, types.orElseThrow());
		var app = runner.load(code);
		app.run();
	}
	
	@Test
	public void declaredTwice() {
		var runner = new MiniPlRunner();
		var src = """
				var n : int;
				var n : string;
				""";
		var prog = runner.parse(src);
		assertEquals(List.of(), runner.errors());
		runner.typeCheck(prog);
		assertEquals(List.of(
				new WrappedError(SemanticError.of(SemanticError.Type.VARIABLE_ALREADY_DEFINED, "n"), Integer.MAX_VALUE)
				), runner.errors());
		assertThrows(IllegalStateException.class, () -> runner.compile(null, null));
	}
	
	@Test
	public void useBeforeDeclaration() {
		var runner = new MiniPlRunner();
		var src = """
				print n;
				var n : int;
				""";
		var prog = runner.parse(src);
		assertEquals(List.of(), runner.errors());
		runner.typeCheck(prog);
		assertEquals(List.of(
				new WrappedError(SemanticError.of(SemanticError.Type.VARIABLE_NOT_DEFINED, "n"), Integer.MAX_VALUE)
				), runner.errors());
		assertThrows(IllegalStateException.class, () -> runner.compile(null, null));
	}
}
