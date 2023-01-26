package fi.benjami.code4jvm.annotation;

import java.util.List;
import java.util.Map;

import fi.benjami.code4jvm.Constant;
import fi.benjami.code4jvm.Type;

public interface AnnotationTarget {
	
	void annotate(Annotation annotation);
	
	default void annotate(Type annotation, Map<String, Constant> args, boolean visibleRuntime) {
		annotate(new Annotation(annotation, args, visibleRuntime));
	}
	
	List<Annotation> annotations();
}
