package fi.benjami.code4jvm.test;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import fi.benjami.code4jvm.internal.DebugOptions;

public class EnableDebugExtension implements BeforeAllCallback {

	@Override
	public void beforeAll(ExtensionContext context) throws Exception {
		DebugOptions.ASM_CHECKS = true;
		// Do not print methods, way too much noise with parameterized tests!
	}

}
