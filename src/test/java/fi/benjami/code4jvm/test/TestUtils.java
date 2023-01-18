package fi.benjami.code4jvm.test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import fi.benjami.code4jvm.config.CompileOptions;
import fi.benjami.code4jvm.typedef.ClassDef;

public class TestUtils {

	private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
	
	private static final Path TEST_CLASSES = Path.of("build", "test-classes");
	
	public static final MethodHandles.Lookup loadHidden(ClassDef def, CompileOptions opts) throws IllegalAccessException, IOException {
		var code = def.compile(opts);
		var code2 = def.compile(opts);
		// Make sure compilation results are stable when options don't change
		assertArrayEquals(code, code2);
		
		// Use short name for class that was compiled with default options
		var fileId = def.type().simpleName() + (opts.equals(CompileOptions.DEFAULT) ? "" : opts.toString().substring(14));
		Files.createDirectories(TEST_CLASSES);
		Files.write(TEST_CLASSES.resolve(fileId + ".class"), code,
				StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
		
		return LOOKUP.defineHiddenClass(code, true);
	}
	
	public static final Object newInstance(MethodHandles.Lookup lookup) throws Throwable {
		return lookup.findConstructor(lookup.lookupClass(), MethodType.methodType(void.class)).invoke();
	}
	
	public static final Object newInstance(ClassDef def, CompileOptions opts) throws Throwable {
		var lookup = loadHidden(def, opts);
		return newInstance(lookup);
	}
}
