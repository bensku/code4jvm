package fi.benjami.parserkit.parser;

import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import fi.benjami.parserkit.minipl.MiniPlTokenType;

public class ParserTest {

	@Test
	public void emptyParser() {
		var parser = Parser.compileAndLoad(new NodeRegistry(), MiniPlTokenType.values());
		assertNull(parser.parse(null, null));
	}
}
