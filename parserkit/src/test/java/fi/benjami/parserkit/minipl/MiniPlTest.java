package fi.benjami.parserkit.minipl;

import org.junit.jupiter.api.Test;

import fi.benjami.parserkit.lexer.TokenizedText;

public class MiniPlTest {

	@Test
	public void sample1() {
		var code = """
					var X : int := 4 + (6 * 2);
					print X;
					""";
		var text = new TokenizedText(new HandWrittenLexer());
		var view = text.apply(code, 0, 0);
//		while (view.hasNext()) {
//			System.out.println(view.pop());
//		}
		
		var view2 = text.apply("", 16, 22);
		var view3 = text.viewFromStart();
		while (view3.hasNext()) {
			System.out.println(view3.pop());
		}
	}
	
	@Test
	public void sample2() {
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
