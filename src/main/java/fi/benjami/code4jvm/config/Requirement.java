package fi.benjami.code4jvm.config;

import fi.benjami.code4jvm.Condition;

public record Requirement<A, B>(
		A ourValue,
		CompileOption<B> otherOption,
		Condition.Type compareOp,
		B expected
) {
	
	public boolean check(Object actual) {
		return switch (compareOp) {
		case REF_EQUAL -> expected == actual;
		case REF_NOT_EQUAL -> expected != actual;
		case EQUAL -> expected.equals(actual);
		case NOT_EQUAL -> !expected.equals(actual);
		case GREATER_THAN -> compare(expected, actual) > 0;
		case LESS_THAN -> compare(expected, actual) < 0;
		case GREATER_OR_EQUAL -> compare(expected, actual) >= 0;
		case LESS_OR_EQUAL -> compare(expected, actual) <= 0;
		// Nulls are not allowed
		case NULL -> false;
		case NOT_NULL -> true;
		// Boolean comparisons don't actually use the "expected" value at all
		case TRUE -> actual.equals(true);
		case FALSE -> actual.equals(false);
		case ALWAYS_TRUE -> true;
		case ALWAYS_FALSE -> false;
		};
	}
	
	@SuppressWarnings("unchecked")
	private static int compare(Object lhs, Object rhs) {
		var a = (Comparable<Object>) lhs;
		var b = (Comparable<Object>) rhs;
		return a.compareTo(b);
	}
}
