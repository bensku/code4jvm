package fi.benjami.code4jvm;

import fi.benjami.code4jvm.annotation.AnnotationTarget;
import fi.benjami.code4jvm.flag.Access;
import fi.benjami.code4jvm.flag.FieldFlag;

public class Field implements AnnotationTarget {

	private final Access access;
	private final Type type;
	private final String name;
	private final FieldFlag[] flags;
	
	private final Constant initialValue;
}
