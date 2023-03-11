package fi.benjami.parserkit.minipl;

import fi.benjami.parserkit.lexer.Token;
import fi.benjami.parserkit.parser.ast.AstNode;
import fi.benjami.parserkit.parser.internal.ParserHook;

public class TestParserHook implements ParserHook {

	@Override
	public void token(int expected, Token actual) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void beforeChoices(int nextToken, String desc) {
		System.out.println("choices: " + desc);
		System.out.println("next token: " + nextToken);
	}

	@Override
	public void choiceToken(int type, String desc) {
		System.out.println("predict token: " + MiniPlTokenType.values()[type]);
		System.out.println("valid choices: " + desc);
	}
	
	@Override
	public void choiceBeforeInput(String desc) {
		System.out.println("choice: " + desc);
	}

	@Override
	public void choiceAfterInput(String desc, boolean success) {
		System.out.println("end choice: " + desc + ", " + success);
	}

	@Override
	public void afterChoices(String desc, boolean success) {
		System.out.println("end choices: " + success);
	}

	@Override
	public void beforeCompound(String desc) {
		System.out.println("compound: " + desc);
	}
	
	@Override
	public void compoundBeforePart(String desc, int index) {
		System.out.println("part: " + index);
	}

	@Override
	public void compoundAfterPart(String desc, int index, boolean success) {
		System.out.println("part success: " + index);
	}

	@Override
	public void afterCompound(String desc, boolean success) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void beforeRepeating(String desc) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void afterRepeating(String desc, boolean success) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void beforeChildNode(Class<? extends AstNode> type, boolean blacklisted) {
		System.out.println("node: " + type + ", blacklist: " + blacklisted);
	}

	@Override
	public void afterChildNode(Class<? extends AstNode> type, boolean success) {
		// TODO Auto-generated method stub
		
	}

}
