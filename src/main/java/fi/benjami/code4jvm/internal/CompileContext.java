package fi.benjami.code4jvm.internal;

import org.objectweb.asm.MethodVisitor;

/**
 * Context for compiling a single method.
 *
 */
public record CompileContext(
		MethodVisitor asm,
		SlotAllocator slotAllocator
) {}
