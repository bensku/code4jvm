package fi.benjami.parserkit.minipl;

import fi.benjami.parserkit.lexer.Lexer;
import fi.benjami.parserkit.lexer.LexerInput;
import fi.benjami.parserkit.lexer.Token;

public class HandWrittenLexer implements Lexer {

	@Override
	public Token getToken(LexerInput input) {
		skipWhitespace(input);
		
		if (input.codepointsLeft() == 0) {
			return null;
		}
		
		var ch = input.getCodepoint(0);
		var pos = input.pos();
		var token = switch (ch) {
		case '+' -> MiniPlTokenType.ADD.read(pos, "+");
		case '-' -> MiniPlTokenType.SUBTRACT.read(pos, "-");
		case '*' -> MiniPlTokenType.MULTIPLY.read(pos, "*");
		case '/' -> {
			var next = input.getCodepoint(1);
			if (next == '/') {
				// Single-line comment
				var id = new StringBuilder();
				for (int i = 2; i < input.codepointsLeft(); i++) {
					next = input.getCodepoint(i);
					if (next == '\n') {
						break;
					}
					id.appendCodePoint(next);
				}
				yield MiniPlTokenType.COMMENT.read(pos, id.toString(), id.length() + 3); // //\n
			} else if (next == '*') {
				// Multi-line comment
				yield null;
			} else {
				yield MiniPlTokenType.DIVIDE.read(pos, "/");
			}
		}
		case '(' -> MiniPlTokenType.GROUP_BEGIN.read(pos, "(");
		case ')' -> MiniPlTokenType.GROUP_END.read(pos, ")");
		case '&' -> MiniPlTokenType.LOGICAL_AND.read(pos, "&");
		case '!' -> MiniPlTokenType.LOGICAL_NOT.read(pos, "!");
		case '=' -> MiniPlTokenType.EQUALS.read(pos, "=");
		case '<' -> MiniPlTokenType.LESS_THAN.read(pos, "<");
		case ';' -> MiniPlTokenType.STATEMENT_END.read(pos, ";");
		case ':' -> {
			var next = input.getCodepoint(1);
			if (next == '=') {
				yield MiniPlTokenType.ASSIGNMENT.read(pos, ":=");
			} else {
				yield MiniPlTokenType.VAR_TYPE.read(pos, ":");
			}
		}
		case '.' -> {
			var next = input.getCodepoint(1);
			if (next == '.') {
				yield MiniPlTokenType.FOR_DIVIDER.read(pos, "..");
			} else {
				yield MiniPlTokenType.ERROR.read(pos, ".");
			}
		}
		case '"' -> {
			var id = new StringBuilder();
			var escape = false;
			for (int i = 1; i < input.codepointsLeft(); i++) {
				var next = input.getCodepoint(i);
				if (next == '\\') {
					escape = true;
				} else if (escape) {
					// TODO what about illegal escape sequences?
					escape = false;
				} else if (next == '"') {
					break;
				}
				id.appendCodePoint(next);
			}
			yield MiniPlTokenType.STRING_LITERAL.read(pos, id.toString(), id.length() + 2);
		}
		default -> {
			if (Character.isLetter(ch)) {
				var id = new StringBuilder();
				id.appendCodePoint(ch);
				for (int i = 1; i < input.codepointsLeft(); i++) {
					var next = input.getCodepoint(i);
					if (next == '_' || Character.isLetter(next) || Character.isDigit(next)) {
						id.appendCodePoint(next);
					} else {
						break;
					}
				}
				yield MiniPlTokenType.IDENTIFIER.read(pos, id.toString());
			} else if (Character.isDigit(ch)) {
				var digits = new StringBuilder();
				digits.appendCodePoint(ch);
				for (int i = 1; i < input.codepointsLeft(); i++) {
					var next = input.getCodepoint(i);
					if (Character.isDigit(next)) {
						digits.appendCodePoint(next);
					} else {
						break;
					}
				}
				yield MiniPlTokenType.INT_LITERAL.read(pos, digits.toString());
			} else {
				yield MiniPlTokenType.ERROR.read(pos, Character.toString(ch));
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
