package fi.benjami.parserkit.parser;

import java.util.BitSet;

import fi.benjami.parserkit.lexer.TokenType;

public class PredictSet {
	
	public static PredictSet of(TokenType type) {
		var set = new BitSet();
		set.set(type.ordinal());
		return new PredictSet(set);
	}
	
	public static PredictSet everything() {
		return new PredictSet(null);
	}
	
	public static PredictSet nothing() {
		return new PredictSet(new BitSet());
	}

	private BitSet predictions;
	
	private PredictSet(BitSet predictions) {
		this.predictions = predictions;
	}
	
	public PredictSet() {
		this(new BitSet());
	}
	
	public boolean isEverything() {
		return predictions == null;
	}
	
	public void add(PredictSet other) {
		if (other.isEverything()) {
			predictions = null;
		} else if (!isEverything()) {
			predictions.or(other.predictions);
		} // else: do nothing, this already predicts everything
	}
	
	public boolean has(TokenType type) {
		return predictions == null || predictions.get(type.ordinal());
	}
}
