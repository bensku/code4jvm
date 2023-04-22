package fi.benjami.parserkit.parser.internal;

import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.call.CallTarget;
import fi.benjami.parserkit.lexer.Token;
import fi.benjami.parserkit.parser.ast.AstNode;

/**
 * A hook that can be injected to {@link ParserGenerator generated} parsers
 * for debugging purposes.
 *
 */
public interface ParserHook {
	
	static Type THIS = Type.of(ParserHook.class);

	static CallTarget TOKEN = CallTarget.virtualMethod(THIS, Type.VOID, "token", Type.INT, Type.of(Token.class));
	void token(int expected, Token actual);
	
	static CallTarget BEFORE_CHOICES = CallTarget.virtualMethod(THIS, Type.VOID, "beforeChoices", Type.INT, Type.STRING);
	void beforeChoices(int nextToken, String desc);
	
	static CallTarget CHOICE_TOKEN = CallTarget.virtualMethod(THIS, Type.VOID, "choiceToken", Type.INT, Type.STRING);
	void choiceToken(int type, String desc);
	
	static CallTarget CHOICE_BEFORE_INPUT = CallTarget.virtualMethod(THIS, Type.VOID, "choiceBeforeInput", Type.STRING);
	void choiceBeforeInput(String desc);
	
	static CallTarget CHOICE_AFTER_INPUT = CallTarget.virtualMethod(THIS, Type.VOID, "choiceAfterInput", Type.STRING, Type.BOOLEAN);
	void choiceAfterInput(String desc, boolean success);
	
	static CallTarget AFTER_CHOICES = CallTarget.virtualMethod(THIS, Type.VOID, "afterChoices", Type.STRING, Type.BOOLEAN);
	void afterChoices(String desc, boolean success);
	
	static CallTarget BEFORE_COMPOUND = CallTarget.virtualMethod(THIS, Type.VOID, "beforeCompound", Type.STRING);
	void beforeCompound(String desc);
	
	static CallTarget COMPOUND_BEFORE_PART = CallTarget.virtualMethod(THIS, Type.VOID, "compoundBeforePart", Type.STRING, Type.INT);
	void compoundBeforePart(String desc, int index);
	
	static CallTarget COMPOUND_AFTER_PART = CallTarget.virtualMethod(THIS, Type.VOID, "compoundAfterPart", Type.STRING, Type.INT, Type.BOOLEAN);
	void compoundAfterPart(String desc, int index, boolean success);
	
	void afterCompound(String desc, boolean success);
	
	void beforeRepeating(String desc);
	
	void afterRepeating(String desc, boolean success);
	
	static CallTarget BEFORE_CHILD_NODE = CallTarget.virtualMethod(THIS, Type.VOID, "beforeChildNode",
			Type.of(Class.class), Type.BOOLEAN);
	void beforeChildNode(Class<? extends AstNode> type, boolean blacklisted);
	
	static CallTarget AFTER_CHILD_NODE = CallTarget.virtualMethod(THIS, Type.VOID, "afterChildNode",
			Type.of(Class.class), Type.BOOLEAN);
	void afterChildNode(Class<? extends AstNode> type, boolean success);
}
