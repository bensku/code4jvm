package fi.benjami.parserkit.minipl;

import fi.benjami.parserkit.lexer.Lexer;
import fi.benjami.parserkit.lexer.LexerInput;
import fi.benjami.parserkit.lexer.Token;
import fi.benjami.parserkit.minipl.token.ArithmeticToken;
import fi.benjami.parserkit.minipl.token.AssignmentToken;
import fi.benjami.parserkit.minipl.token.EndToken;
import fi.benjami.parserkit.minipl.token.ErrorToken;
import fi.benjami.parserkit.minipl.token.IdentifierToken;
import fi.benjami.parserkit.minipl.token.IntLiteralToken;
import fi.benjami.parserkit.minipl.token.LogicalOpToken;
import fi.benjami.parserkit.minipl.token.StatementEndToken;
import fi.benjami.parserkit.minipl.token.VarTypeToken;

public class HandWrittenLexer implements Lexer {

	@Override
	public Token getToken(LexerInput input) {
		skipWhitespace(input);
		
		if (input.codepointsLeft() == 0) {
			return new EndToken(input.pos());
		}
		
		var ch = input.getCodepoint(0);
		var pos = input.pos(); 
		var token = switch (ch) {
		case '+' -> new ArithmeticToken(pos, 1, ArithmeticToken.Op.ADD);
		case '-' -> new ArithmeticToken(pos, 1, ArithmeticToken.Op.SUBTRACT);
		case '*' -> new ArithmeticToken(pos, 1, ArithmeticToken.Op.MULTIPLY);
		case '/' -> {
			var next = input.getCodepoint(1);
			if (next == '/') {
				// Single-line comment
				yield null;
			} else if (next == '*') {
				// Multi-line comment
				yield null;
			} else {
				yield new ArithmeticToken(pos, 1, ArithmeticToken.Op.DIVIDE);
			}
		}
		case '(' -> new ArithmeticToken(pos, 1, ArithmeticToken.Op.GROUP_BEGIN);
		case ')' -> new ArithmeticToken(pos, 1, ArithmeticToken.Op.GROUP_END);
		case '&' -> new LogicalOpToken(pos, 1, LogicalOpToken.Type.AND);
		case '!' -> new LogicalOpToken(pos, 1, LogicalOpToken.Type.NOT);
		case '=' -> new LogicalOpToken(pos, 1, LogicalOpToken.Type.EQUAL);
		case '<' -> new LogicalOpToken(pos, 1, LogicalOpToken.Type.LESS_THAN);
		case ';' -> new StatementEndToken(pos);
		case ':' -> {
			var next = input.getCodepoint(1);
			if (next == '=') {
				yield new AssignmentToken(pos);
			} else {
				yield new VarTypeToken(pos);
			}
		}
//		case '"' -> {
//			// TODO string handling
//			yield null;
//		}
		default -> {
			if (Character.isLetter(ch)) {
				var id = new StringBuilder();
				id.appendCodePoint(ch);
				for (int i = 1;; i++) {
					var next = input.getCodepoint(i);
					if (next == '_' || Character.isLetter(next) || Character.isDigit(next)) {
						id.appendCodePoint(next);
					} else {
						break;
					}
				}
				yield new IdentifierToken(pos, id.toString());
			} else if (Character.isDigit(ch)) {
				var digits = new StringBuilder();
				digits.appendCodePoint(ch);
				for (int i = 1;; i++) {
					var next = input.getCodepoint(i);
					if (Character.isDigit(next)) {
						digits.appendCodePoint(next);
					} else {
						break;
					}
				}
				yield new IntLiteralToken(pos, digits.toString());
			} else {
				yield new ErrorToken(pos, 1, Character.toString(ch));
			}
		}
		};
		input.advance(token.length());
		return token;
	}

	@Override
	public boolean isWhitespace(int ch) {
		return LexerTools.isWhitespace(ch);
	}

}
