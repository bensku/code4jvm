package fi.benjami.parserkit.minipl;

public record WrappedError(Object error, int pos) implements Comparable<WrappedError> {

	@Override
	public int compareTo(WrappedError o) {
		return Integer.compare(pos, o.pos);
	}
	
}
