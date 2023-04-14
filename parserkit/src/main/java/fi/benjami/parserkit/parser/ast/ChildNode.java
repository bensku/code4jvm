package fi.benjami.parserkit.parser.ast;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface ChildNode {

	String value();
}
