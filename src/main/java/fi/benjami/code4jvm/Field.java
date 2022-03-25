package fi.benjami.code4jvm;

public class Field {

	private final Type definingClass;
	private final Type type;
	private final String name;
	
	Field(Type definingClass, Type type, String name) {
		this.definingClass = definingClass;
		this.type = type;
		this.name = name;
	}
	
	public Type definingClass() {
		return definingClass;
	}
	
	public Type type() {
		return type;
	}
	
	public String name() {
		return name;
	}
}
