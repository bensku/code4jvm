package fi.benjami.code4jvm.block;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.TypeReference;
import org.objectweb.asm.util.CheckMethodAdapter;

import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.annotation.Annotation;
import fi.benjami.code4jvm.call.CallTarget;
import fi.benjami.code4jvm.call.FixedCallTarget;
import fi.benjami.code4jvm.config.CompileOptions;
import fi.benjami.code4jvm.config.CoreOptions;
import fi.benjami.code4jvm.flag.Access;
import fi.benjami.code4jvm.internal.DebugOptions;
import fi.benjami.code4jvm.internal.FrameManager;
import fi.benjami.code4jvm.internal.LocalVar;
import fi.benjami.code4jvm.internal.MethodCompilerState;
import fi.benjami.code4jvm.internal.SlotAllocator;
import fi.benjami.code4jvm.typedef.ClassDef;
import fi.benjami.code4jvm.util.TypeUtils;

import static org.objectweb.asm.Opcodes.*;

import java.util.stream.Stream;

/**
 * Compiles methods to JVM bytecode with ASM.
 * 
 * @see Method
 * @see ClassDef
 *
 */
public class MethodCompiler {
	
	private final ClassDef owner;
	private final ClassVisitor cv;
	private final CompileOptions options;
	
	public MethodCompiler(ClassDef owner, ClassVisitor cv, CompileOptions options) {
		this.owner = owner;
		this.cv = cv;
		this.options = options;
	}

	public void compile(Method method, Access access) {
		var argTypes = method.argumentTypes().toArray(Type[]::new);
		// access int is not public API, so we'll need to dig it up from implementation
		var accessBits = method instanceof ConcreteMethod concrete ? concrete.access
				: ((AbstractMethod) method).access;
		accessBits |= access.value();
		
		var mv = cv.visitMethod(accessBits, method.name(),
				TypeUtils.methodDescriptor(method.returnType(), argTypes), null, null);
		if (DebugOptions.ASM_CHECKS) {
			var checker = new CheckMethodAdapter(mv);
			// CheckMethodAdapter needs version set to accept newer functionality
			checker.version = options.get(CoreOptions.JAVA_VERSION).opcode();
			mv = checker;
		}
		
		if (method instanceof ConcreteMethod concrete) {
			var slotAllocator = concrete.slotAllocator;
			if (slotAllocator == null) {
				slotAllocator = new SlotAllocator();
				concrete.slotAllocator = slotAllocator;
			}
			// Assign local variable slots for arguments
			if (method instanceof Method.Instance instance) {
				slotAllocator.assignSlot(instance.self); // this is always slot 0
			}
			slotAllocator.assignArgSlots(concrete.args);
			
			// Compute stack map table frames (and assign rest of local variables slots)
			if (!concrete.framesComputed) {
				new FrameBuilder(slotAllocator, concrete).trace();
				concrete.framesComputed = true;
				if (DebugOptions.PRINT_METHODS) {
					// JVM property code4jvm.debug.printMethods
					System.out.println(concrete);
				}
			}
			
			var ctx = new CompileContext(owner, getTarget(accessBits, method.returnType(), method.name(), argTypes), mv, options);
			var state = new MethodCompilerState(ctx, slotAllocator, new FrameManager(method, slotAllocator), options.get(CoreOptions.LOCAL_VAR_TABLE));
			
			
			// Emit method content
			mv.visitCode();
			concrete.block().emitBytecode(state);
			
			// Emit local variable table if it is enabled
			slotAllocator.variables();
			
			// Set stack and 
			mv.visitMaxs(ctx.stack().maxStackSize(), slotAllocator.slotCount());
		}
		mv.visitEnd();
	}

	private FixedCallTarget getTarget(int access, Type returnType, String name, Type[] argTypes) {
		// Static method, static linkage
		if ((access & ACC_STATIC) != 0) {
			return CallTarget.staticMethod(owner.type(), returnType, name, argTypes);
		}
		
		// Special linkage
		if (name.equals("<init>")) {
			return CallTarget.constructor(owner.type(), argTypes);
		} else if ((access & ACC_PRIVATE) != 0) {
			return CallTarget.privateMethod(owner.type(), returnType, name, argTypes);
		}
		
		// Virtual/interface linkage depending on owner type
		return CallTarget.virtualMethod(owner.type(), returnType, name, argTypes);
	}
	
	private void emitVariableMetadata(MethodCompilerState state, Stream<LocalVar> variables) {
		var mv = state.ctx().asm();
		variables.forEach(localVar -> {
			var annotations = localVar.annotations();
			var methodArg = state.slotAllocator().isMethodArg(localVar);
			
			// Emit method argument name, modifiers and annotations
			if (methodArg) {
				// TODO final, synthetic, mandated (last of which is also missing from other places)
				mv.visitParameter(localVar.name().orElse(null), 0);
				for (Annotation ann : annotations) {
					// FIXME method parameter count, this is NOT SAME as slot
					mv.visitParameterAnnotation(-1, ann.type().descriptor(), ann.visibleRuntime());
				}
			}
			
			// If local variable table is enabled, generate it for variables for which
			// we have enough information
			if (state.emitVarMarkers() && localVar.needsSlot && localVar.definitionStart != null) {
				var name = localVar.name().orElse("<unnamed>");
				mv.visitLocalVariable(name, localVar.type().descriptor(), null, localVar.definitionStart,
						localVar.definitionEnd, localVar.assignedSlot);
				if (!methodArg) {
					// Local variable, not method argument -> add annotations to table
					for (Annotation ann : annotations) {
						// TODO RESOURCE_VARIABLE?
						// TODO array member annotations?
						// TODO runtime visibility?
						// We're not reusing slots, which makes this much easier
						mv.visitLocalVariableAnnotation(TypeReference.LOCAL_VARIABLE, null,
								new Label[] {localVar.definitionStart}, new Label[] {localVar.definitionEnd},
								new int[] {localVar.assignedSlot}, ann.type().descriptor(), ann.visibleRuntime());
					}
				}
			}
			
		});
	}

}
