package fi.benjami.code4jvm.lua.compiler;

import java.lang.invoke.MethodHandle;

public record CompiledFunction(
		MethodHandle constructor,
		MethodHandle function
) {}
