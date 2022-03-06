package fi.benjami.code4jvm;

public interface Variable extends Value {
	
	Statement set(Value value);
}
