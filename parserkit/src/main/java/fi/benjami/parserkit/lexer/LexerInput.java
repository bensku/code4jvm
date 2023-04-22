package fi.benjami.parserkit.lexer;

public class LexerInput {

	private final String text;
	private int pos;
	
	private int error;
	
	public LexerInput(String text, int startPos) {
		this.text = text;
		this.pos = startPos;
	}
	
	public void advance(int count) {
		assert count > 0;
		pos += count;
	}
	
	public int getCodepoint(int offset) {
		return text.codePointAt(pos + offset);
	}
	
	public int codepointsLeft() {
		return text.length() - pos;
	}
	
	public int pos() {
		return pos;
	}
	
	public void setError(int error) {
		this.error = error;
	}
	
	public int clearError() {
		return error;
	}
}
