package fi.benjami.code4jvm.lua.parser;

import fi.benjami.parserkit.lexer.Lexer;
import fi.benjami.parserkit.lexer.LexerInput;
import fi.benjami.parserkit.lexer.Token;

public class LuaLexer implements Lexer {

	@Override
	public Token getToken(LexerInput input) {
		var error = input.clearError();
		if (error != 0) {
			// TODO use the error code
			return LuaToken.ERROR.read(input.pos(), "");
		}
		
		skipWhitespace(input);
		if (input.codepointsLeft() == 0) {
			return null;
		}
		
		// This is essentially hand-written table based lexer
		// The "table" is constructed using nested switch expressions
		// and identifiers/literals are parsed separately
		var ch = input.getCodepoint(0);
		var pos = input.pos();
		var token = switch (ch) {
		case '(' -> LuaToken.GROUP_BEGIN.read(pos, "(");
		case ')' -> LuaToken.GROUP_END.read(pos, ")");
		case '[' -> LuaToken.TABLE_INDEX_BEGIN.read(pos, "[");
		case ']' -> LuaToken.TABLE_INDEX_END.read(pos, "]");
		case ';' -> LuaToken.STATEMENT_END.read(pos, ";");
		case ':' -> switch (input.getCodepoint(1)) {
			case ':' -> LuaToken.GOTO_LABEL.read(pos, "::");
			default -> LuaToken.OOP_FUNC_SEPARATOR.read(pos, ":");
		};
		case '=' -> switch (input.getCodepoint(1)) {
			case '=' -> LuaToken.EQUAL.read(pos, "==");
			default -> LuaToken.ASSIGNMENT.read(pos, "=");
		};
		case '.' -> switch (input.getCodepoint(1)) {
			// TODO .5 style decimal numbers
			case '.' -> switch (input.getCodepoint(2)) {
				case '.' -> LuaToken.VARARGS.read(pos, "...");
				default -> LuaToken.STRING_CONCAT.read(pos, "..");
			};
			default -> LuaToken.NAME_SEPARATOR.read(pos, ".");
		};
		case ',' -> LuaToken.LIST_SEPARATOR.read(pos, ",");
		case '+' -> LuaToken.ADD.read(pos, "+");
		case '-' -> LuaToken.SUBTRACT_OR_NEGATE.read(pos, "-");
		case '*' -> LuaToken.MULTIPLY.read(pos, "*");
		case '/' -> switch (input.getCodepoint(1)) {
			case '/' -> LuaToken.FLOOR_DIVIDE.read(pos, "//");
			default -> LuaToken.DIVIDE.read(pos, "/");
		};
		case '^' -> LuaToken.POWER.read(pos, "^");
		case '%' -> LuaToken.MODULO.read(pos, "%");
		case '&' -> LuaToken.BITWISE_AND.read(pos, "&");
		case '~' -> switch (input.getCodepoint(1)) {
			case '=' -> LuaToken.NOT_EQUAL.read(pos, "~=");
			default -> LuaToken.BITWISE_XOR_OR_NOT.read(pos, "~");
		};
		case '|' -> LuaToken.BITWISE_OR.read(pos, "|");
		case '>' -> switch (input.getCodepoint(1)) {
			case '>' -> LuaToken.BIT_SHIFT_RIGHT.read(pos, ">>");
			case '=' -> LuaToken.MORE_OR_EQUAL.read(pos, ">=");
			default -> LuaToken.MORE_THAN.read(pos, ">");
		};
		case '<' -> switch (input.getCodepoint(1)) {
			case '<' -> LuaToken.BIT_SHIFT_LEFT.read(pos, "<<");
			case '=' -> LuaToken.LESS_OR_EQUAL.read(pos, "<=");
			default -> LuaToken.LESS_THAN.read(pos, "<");
		};
		case '#' -> LuaToken.ARRAY_LENGTH.read(pos, "#");
		case '{' -> LuaToken.TABLE_INIT_START.read(pos, "{");
		case '}' -> LuaToken.TABLE_INIT_END.read(pos, "}");
		case '"' -> parseShortString('"', input);
		case '\'' -> parseShortString('\'', input);
		// TODO long strings, i.e. [==[ ]==]
		default -> {
			if (isNumber(ch)) {
				yield parseNumber(input);
			} else if (isLetter(ch)) {
				yield parseName(input);
			} else {
				yield LuaToken.ERROR.read(pos, "" + ch);
			}
		}
		};
		input.advance(token.length());
		
		return token;
	}
	
	private Token parseShortString(int startChar, LexerInput input) {
		var str = new StringBuilder();
		
		var escape = false;
		for (var i = 1; i < input.codepointsLeft(); i++) {
			var c = input.getCodepoint(i);
			
			if (escape) {
				switch (c) {
				// Control characters - Lua supports several more than Java
				case 'a' -> str.appendCodePoint(7); // bell
				case 'b' -> str.append('\b');
				case 'f' -> str.append('\f');
				case 'n' -> str.append('\n');
				case 'r' -> str.append('\r');
				case 't' -> str.append('\t');
				case 'v' -> str.appendCodePoint(11); // vertical tab
				// Characters that can't appear in (all) string literals as-is
				case '\\' -> str.append('\\');
				case '"' -> str.append('"');
				case '\'' -> str.append('\'');
				// Line-break can be escaped as-is
				case '\n' -> str.append('\n');
				// \z is weird, skipping multiple following characters
				case 'z' -> throw new UnsupportedOperationException();
				default -> {
					// Invalid escape, produce error
					input.setError(2); // TODO how to set error in middle of string?
					break;
				}
				}
				
				escape = false;
				continue;
			}
			
			if (c == '\\') {
				escape = true;
			} else if (c == '\n') {
				// Invalid, unescaped line break
				// End string, but produce an error token immediately after it
				input.setError(1); // TODO real error id
				break;
			} else if (c == startChar) {
				// Valid string end
				break;
			} else {
				str.appendCodePoint(c);
			}
		}
		
		return LuaToken.STRING_LITERAL.read(input.pos(), str.toString(), str.length() + 2);
	}
	
	private Token parseNumber(LexerInput input) {
		var str = new StringBuilder();
		
		var decimalDots = 0;
		for (var i = 0; i < input.codepointsLeft(); i++) {
			var c = input.getCodepoint(i);
			if (isNumber(c)) {
				str.appendCodePoint(c);
			} else if (c == '.') {
				decimalDots++;
				str.appendCodePoint(c);
			} else {
				break;
			}
		}
		
		if (decimalDots > 1) {
			return LuaToken.ERROR.read(input.pos(), str.toString());
		} else {			
			return LuaToken.LITERAL_NUMBER.read(input.pos(), str.toString());
		}
	}
	
	private Token parseName(LexerInput input) {
		var str = new StringBuilder();
		
		for (var i = 0; i < input.codepointsLeft(); i++) {
			var c = input.getCodepoint(i);
			// We won't even be called if first character is not letter
			// -> no need to have separate code path for it being number
			if (isLetter(c) || isNumber(c)) {
				str.appendCodePoint(c);
			} else {
				break;
			}
		}
		
		
		return LuaToken.NAME.read(input.pos(), str.toString());
	}
	
	private boolean isLetter(int ch) {
		return (ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z') || ch == '_';
	}
	
	private boolean isNumber(int ch) {
		return ch >= '0' && ch <= '9';
	}

	@Override
	public boolean isWhitespace(int ch) {
		// Lua spec defines whitespace to be these characters only
		return ch == ' ' || (ch >= '\t' && ch <= '\r');
	}

}
