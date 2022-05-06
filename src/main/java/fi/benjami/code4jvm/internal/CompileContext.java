package fi.benjami.code4jvm.internal;

import org.objectweb.asm.MethodVisitor;

import fi.benjami.code4jvm.ClassDef;

/**
 * Context for compiling a single method.
 *
 */
public record CompileContext(
		ClassDef owner,
		MethodVisitor asm,
		SlotAllocator slotAllocator
) {}
