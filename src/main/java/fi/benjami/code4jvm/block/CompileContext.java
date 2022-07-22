package fi.benjami.code4jvm.block;

import org.objectweb.asm.MethodVisitor;

import fi.benjami.code4jvm.CompileOptions;
import fi.benjami.code4jvm.call.FixedCallTarget;
import fi.benjami.code4jvm.typedef.ClassDef;

/**
 * Context for compiling a single method.
 *
 */
public record CompileContext(
		/**
		 * Type that is being compiled.
		 */
		ClassDef owner,
		
		/**
		 * Call target that refers to the method that is being compiled.
		 */
		FixedCallTarget method,
		
		/**
		 * ASM visitor used for emitting method bytecode.
		 */
		MethodVisitor asm,
		
		/**
		 * Compiler options used for the class that the method is part of.
		 */
		CompileOptions options
) {}
