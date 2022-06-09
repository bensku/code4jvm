package fi.benjami.code4jvm.block;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.util.CheckMethodAdapter;

import fi.benjami.code4jvm.ClassDef;
import fi.benjami.code4jvm.CompileOptions;
import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.Value;
import fi.benjami.code4jvm.call.CallTarget;
import fi.benjami.code4jvm.call.FixedCallTarget;
import fi.benjami.code4jvm.flag.Access;
import fi.benjami.code4jvm.internal.MethodCompilerState;
import fi.benjami.code4jvm.internal.SlotAllocator;
import fi.benjami.code4jvm.util.TypeUtils;

import static org.objectweb.asm.Opcodes.*;

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
		var argTypes = method.args.stream().map(Value::type).toArray(Type[]::new);
		var mv = cv.visitMethod(method.access | access.value(), method.name(),
				TypeUtils.methodDescriptor(method.returnType(), argTypes), null, null);
		if (options.asmChecks()) {
			mv = new CheckMethodAdapter(mv);
		}
		
		var ctx = new CompileContext(owner, getTarget(method.access, method.returnType(), method.name(), argTypes), mv, options);
		// Start allocating other values after this references + args
		var state = new MethodCompilerState(ctx, new SlotAllocator(method.argsAllocator));
				
		// Emit method content
		// TODO support abstract methods?
		mv.visitCode();
		method.block().emitBytecode(state, null);
		mv.visitEnd();
		mv.visitMaxs(0, 0);
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
}
