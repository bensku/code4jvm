package fi.benjami.code4jvm.internal;

public class DebugOptions {

	public static boolean ASM_CHECKS = option("asmChecks");
	
	private static boolean option(String name) {
		var prop = System.getProperty("code4jvm.debug." + name);
		return prop != null && prop.equals("true");
	}
	
	public static void enableAll() {
		ASM_CHECKS = true;
	}
}
