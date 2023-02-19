package fi.benjami.parserkit.lexer;

import java.util.Objects;

public class Token {
	
	private int start;
	private final int length;
	
	private final int type;
	private final Object value;
		
	public Token(int start, int length, int type, Object value) {
		this.start = start;
		this.length = length;
		this.type = type;
		this.value = value;
	}
	
	public int start() {
		return start;
	}
	
	public void modifyStart(int mod) {
		start += mod;
	}
	
	public int length() {
		return length;
	}
	
	public int end() {
		return start + length;
	}
	
	public int type() {
		return type;
	}
	
	public Object value() {
		return value;
	}
	
	@Override
	public boolean equals(Object o) {
		return o instanceof Token token
				&& token.start == start
				&& token.length == length
				&& token.type == type
				&& Objects.equals(token.value, value);
	}
	
	@Override
	public String toString() {
		return "Token{start=" + start + ",length=" + length + ",type=" + type + ",value=" + value + "}";
	}

}
