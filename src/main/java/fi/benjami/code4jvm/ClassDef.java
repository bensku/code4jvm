package fi.benjami.code4jvm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.CheckClassAdapter;

import fi.benjami.code4jvm.flag.MethodFlag;
import fi.benjami.code4jvm.statement.Return;
import fi.benjami.code4jvm.flag.Access;
import fi.benjami.code4jvm.flag.ClassFlag;

public class ClassDef {
	
	public static ClassDef create(String name, Access access, ClassFlag... flags) {
		var acc = access.value();
		for (var flag : flags) {
			acc |= flag.value();
		}
		return new ClassDef(acc, name);
	}

	private final int access;
	private final String name;
	private final Type type;
	
	private Type superClass;
	private Type[] interfaces;
	
	private final List<Method> methods;
	
	private ClassDef(int access, String name) {
		this.access = access;
		this.name = name;
		this.type = Type.of(name, (access & Opcodes.ACC_INTERFACE) != 0);
		this.methods = new ArrayList<>();
	}
	
	public Type type() {
		return type;
	}
	
	public void superClass(Type type) {
		if (superClass != null) {
			throw new IllegalStateException("already extends " + superClass);
		}
		superClass = type;
	}
	
	public void interfaces(Type... types) {
		if (interfaces != null) {
			throw new IllegalStateException("already implements interfaces");
		}
		interfaces = types;
	}
	
	public Method.Instance addMethod(Type returnType, String name, Access access, MethodFlag... flags) {
		var method = new Method.Instance(returnType, name, type());
		method.setAccess(access);
		method.setFlags(flags);
		methods.add(method);
		return method;
	}
	
	public Method.Instance addConstructor(Access access, MethodFlag... flags) {
		return addMethod(Type.VOID, "<init>", access, flags);
	}
	
	public void addEmptyConstructor(Access access) {
		var superType = superClass != null ? superClass : Type.OBJECT;
		var constructor = addConstructor(access);
		constructor.add(constructor.self().callSpecial(superType, Type.VOID, "<init>"));
		constructor.add(Return.nothing());
	}
	
	public Method.Static addStaticMethod(Type returnType, String name, Access access, MethodFlag... flags) {
		var method = new Method.Static(returnType, name);
		method.setAccess(access);
		method.setFlags(flags);
		method.markStatic();
		return method;
	}
	
	public byte[] compile(CompileOptions opts) {
		var writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
		// Enable ASM checks if user has requested them
		// But don't enable data flow checks, can't use them with COMPUTE_FRAMES
		var cv = opts.asmChecks() ? new CheckClassAdapter(writer, false) : writer;
		var superName = superClass != null ? superClass.internalName() : "java/lang/Object";
		var interfaceNames = interfaces != null ? 
				Arrays.stream(interfaces).map(Type::internalName).toArray(String[]::new)
				: null;
		// TODO customizable Java version (needs more than just this flag, though)
		cv.visit(Opcodes.V17, access, name.replace('.', '/'), null, superName, interfaceNames);
		
		for (var method : methods) {
			method.compile(cv, opts);
		}
		
		cv.visitEnd();
		return writer.toByteArray();
	}
	
	public byte[] compile() {
		return compile(CompileOptions.DEFAULT);
	}

}
