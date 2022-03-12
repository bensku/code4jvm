package fi.benjami.code4jvm;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.util.CheckMethodAdapter;

import fi.benjami.code4jvm.call.MethodLookup;
import fi.benjami.code4jvm.call.StaticCallTarget;
import fi.benjami.code4jvm.flag.Access;
import fi.benjami.code4jvm.flag.MethodFlag;
import fi.benjami.code4jvm.internal.CompileContext;
import fi.benjami.code4jvm.internal.LocalVar;
import fi.benjami.code4jvm.internal.SlotAllocator;

public class Method extends Block {
	
	public static MethodLookup<StaticCallTarget> staticLookup(Type owner, boolean ownerIsInterface) {
		return MethodLookup.ofStatic(owner, ownerIsInterface);
	}
	
	public static class Static extends Method {
		Static(Type returnType, String name) {
			super(returnType, name);
		}
	}
	
	public static class Instance extends Method {
		
		/**
		 * Value for 'this'.
		 */
		private final LocalVar self;
		
		Instance(Type returnType, String name, Type parentClass) {
			super(returnType, name);
			// this is passed in slot 0
			// It is NOT present in method signature, so avoid calling arg()
			this.self = new LocalVar(parentClass, this);
			self.initialized = true; // this is always initialized
			argsAllocator.get(self);
		}
		
		public Value self() {
			return self;
		}
	}

	private final Type returnType;
	private final String name;
	private int access;
	
	private final List<LocalVar> args;
	final SlotAllocator argsAllocator;
	
	Method(Type returnType, String name) {
		this.returnType = returnType;
		this.name = name;
		this.args = new ArrayList<>();
		this.argsAllocator = new SlotAllocator(null);
	}
	
	void setAccess(Access access) {
		this.access = access.value();
	}
	
	void setFlags(MethodFlag... flags) {
		// JVM stores other flags in access bit set, OR them into it
		for (var flag : flags) {
			this.access |= flag.value();
		}
	}
	
	void markStatic() {
		this.access |= Opcodes.ACC_STATIC;
	}
	
	public Value arg(Type type, String name) {
		var localVar = new LocalVar(type, this);
		localVar.initialized = true; // Arguments are always initialized (but can be null)
		localVar.name(name);
		argsAllocator.get(localVar);
		args.add(localVar);
		return localVar;
	}
	
	public Value arg(Type type) {
		return arg(type, null);
	}
	
	void compile(ClassVisitor cv, CompileOptions opts) {
		var argTypes = args.stream().map(Value::type).toArray(Type[]::new);
		var mv = cv.visitMethod(access, name,
				Type.getMethodDescriptor(returnType, argTypes), null, null);
		if (opts.asmChecks()) {
			mv = new CheckMethodAdapter(mv);
		}
		
		// Start allocating other values after this references + args
		var ctx = new CompileContext(mv, new SlotAllocator(argsAllocator));
		
		// Emit method content
		// TODO support abstract methods?
		mv.visitCode();
		emitBytecode(ctx);
		mv.visitEnd();
		mv.visitMaxs(0, 0);
	}
}
