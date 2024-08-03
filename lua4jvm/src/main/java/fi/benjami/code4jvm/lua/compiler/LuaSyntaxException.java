package fi.benjami.code4jvm.lua.compiler;

import java.util.List;
import java.util.stream.Collectors;

import fi.benjami.code4jvm.lua.LuaVm;

/**
 * Thrown by the {@link LuaVm Lua VM} when source code given to it contains
 * syntax errors. Although the VM will refuse to load such code, it will
 * attempt to parse through the entire chunk and report all identified syntax
 * errors.
 *
 */
public class LuaSyntaxException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final List<Error> errors;
	
	public LuaSyntaxException(List<Error> errors) {
		super(makeMsg(errors));
		if (errors.isEmpty()) {
			throw new IllegalArgumentException("cannot to create syntax error without errors");
		}
		this.errors = errors;
	}
	
	private static String makeMsg(List<Error> errors) {
		return errors.stream()
				.map(error -> error.chunkName + ":" + error.line + ":" + error.posInLine + " " + error.message)
				.collect(Collectors.joining("\n"));
	}
	
	public record Error(
			String chunkName,
			int line,
			int posInLine,
			String message
	) {}
	
	public List<Error> errors() {
		return errors;
	}
}
