package fi.benjami.code4jvm.test;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

import fi.benjami.code4jvm.config.CompileOption;
import fi.benjami.code4jvm.config.CompileOptions;
import fi.benjami.code4jvm.config.CoreOptions;
import fi.benjami.code4jvm.config.JavaVersion;

public class CompilerOptionsProvider implements ArgumentsProvider {

	private static final Boolean[] TOGGLE =  new Boolean[] {true, false};
	private static final List<Object> OPTIONS = List.of(
			CoreOptions.JAVA_VERSION, JavaVersion.values(),
			CoreOptions.LOCAL_VAR_TABLE, TOGGLE
	);
	
	@Override
	public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
		var permutations = new ArrayList<CompileOptions>();
		makeCombinations(permutations, new IdentityHashMap<>(), 0);
		return permutations.stream().map(Arguments::of);
	}
	
	@SuppressWarnings("unchecked")
	private void makeCombinations(List<CompileOptions> out, Map<CompileOption<?>, Object> options, int index) {
		if (index == OPTIONS.size()) {
			var builder = CompileOptions.builder();
			for (var entry : options.entrySet()) {
				builder.set((CompileOption<Object>) entry.getKey(), entry.getValue());
			}
			out.add(builder.build());
			return;
		}
		
		var option = (CompileOption<?>) OPTIONS.get(index);
		var values = (Object[]) OPTIONS.get(index + 1);
		
		for (Object value : values) {
			var childOpts = new IdentityHashMap<>(options);
			childOpts.put(option, value);
			makeCombinations(out, childOpts, index + 2);
		}
	}

}
