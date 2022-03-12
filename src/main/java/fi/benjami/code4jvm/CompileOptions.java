package fi.benjami.code4jvm;

public record CompileOptions(
		boolean asmChecks
) {
	
	public static final CompileOptions DEFAULT = new CompileOptions(false);
}
