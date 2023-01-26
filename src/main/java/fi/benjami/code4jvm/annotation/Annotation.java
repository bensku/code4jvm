package fi.benjami.code4jvm.annotation;

import java.util.Map;

import fi.benjami.code4jvm.Constant;
import fi.benjami.code4jvm.Type;

public record Annotation(
		Type type,
		Map<String, Constant> args,
		boolean visibleRuntime
) {}
