package fi.benjami.code4jvm.statement;

import fi.benjami.code4jvm.Statement;
import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.Value;
import fi.benjami.code4jvm.internal.ReturnImpl;

public class Return {

	public static Statement nothing() {
		return new ReturnImpl(null);
	}
	
	public static Statement value(Value value) {
		if (value.type().equals(Type.VOID)) {
			return nothing();
		}
		return new ReturnImpl(value);
	}
}
