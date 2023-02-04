package fi.benjami.parserkit.lexer;

public abstract class Token {
	
	private int start;
	private final int length;
		
	public Token(int start, int length) {
		this.start = start;
		this.length = length;
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
	
	@Override
	public boolean equals(Object o) {
		return o instanceof Token token && token.start == start && token.length == length;
	}

}
