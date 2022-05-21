package fi.benjami.code4jvm.block;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.util.CheckMethodAdapter;

import fi.benjami.code4jvm.ClassDef;
import fi.benjami.code4jvm.CompileOptions;
import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.Value;
import fi.benjami.code4jvm.flag.Access;
import fi.benjami.code4jvm.internal.CompileContext;
import fi.benjami.code4jvm.internal.SlotAllocator;
import fi.benjami.code4jvm.util.TypeUtils;

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
		
		// Start allocating other values after this references + args
		var ctx = new CompileContext(owner, mv, new SlotAllocator(method.argsAllocator));
		
		// Emit method content
		// TODO support abstract methods?
		mv.visitCode();
		method.block().emitBytecode(ctx);
		mv.visitEnd();
		mv.visitMaxs(0, 0);
	}
}
