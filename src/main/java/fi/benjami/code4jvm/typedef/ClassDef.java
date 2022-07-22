package fi.benjami.code4jvm.typedef;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.CheckClassAdapter;

import fi.benjami.code4jvm.flag.MethodFlag;
import fi.benjami.code4jvm.statement.Return;
import fi.benjami.code4jvm.CompileOptions;
import fi.benjami.code4jvm.Constant;
import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.block.AbstractMethod;
import fi.benjami.code4jvm.block.Method;
import fi.benjami.code4jvm.block.MethodCompiler;
import fi.benjami.code4jvm.flag.Access;
import fi.benjami.code4jvm.flag.ClassFlag;
import fi.benjami.code4jvm.flag.FieldFlag;

public class ClassDef {
	
	public static ClassDef create(String name, Access access, ClassFlag... flags) {
		var acc = access.value();
		for (var flag : flags) {
			acc |= flag.value();
		}
		return new ClassDef(acc, name);
	}
	
	private record Field(boolean isStatic,
			Access access,
			Type type,
			String name,
			FieldFlag[] flags,
			Constant initialValue
	) {}

	private final int access;
	private final String name;
	private final Type type;
	
	private Type superClass;
	private Type[] interfaces;
	
	private final List<Method> methods;
	private final Map<Method, Access> accessTable;
	
	private final List<Field> fields;
	
	ClassDef(int access, String name) {
		this.access = access;
		this.name = name;
		this.type = Type.of(name, (access & Opcodes.ACC_INTERFACE) != 0);
		this.methods = new ArrayList<>();
		this.accessTable = new IdentityHashMap<>();
		this.fields = new ArrayList<>();
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
	
	/**
	 * Adds a method to this class.
	 * @param method Method to add.
	 * @param access Access allowed to the method.
	 * @throws IllegalArgumentException If JVM prohibits adding the method to
	 * this class. Reasons include:
	 * <ul>
	 * <li>The method is abstract, but this is not an abstract class or an interface
	 * <li>This class is an interface, and the method is package-private or
	 * protected (only public and private are allowed)
	 * <li>This class is an interface, and the method is private but not static
	 * <li>This class in an interface, and the method is a constructor
	 * </ul>
	 */
	public void addMethod(Method method, Access access) {
		// Validate that abstract methods (except native) are not added to normal classes
		var acceptsAbstract = (this.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_INTERFACE)) != 0;
		if (!acceptsAbstract
				&& method instanceof AbstractMethod abstractMethod
				&& !abstractMethod.isNative()) {
			throw new IllegalArgumentException("only interfaces and abstract classes may have abstract methods");
		}
		// Validate that interfaces don't get methods not allowed there
		if ((this.access & Opcodes.ACC_INTERFACE) != 0) {
			if (access != Access.PUBLIC && access != Access.PRIVATE) {				
				throw new IllegalArgumentException("interface methods must be public or private");
			}
			if (access == Access.PRIVATE && !(method instanceof Method.Static)) {
				throw new IllegalArgumentException("instance methods of interfaces must be public and not abstract");
			}
			if (method.name().equals("<init>")) {
				throw new IllegalArgumentException("interfaces cannot have constructors");
			}
		}
		
		methods.add(method);
		accessTable.put(method, access);
	}
	
	/**
	 * Creates and adds a non-abstract instance method to this class.
	 * @param returnType Return type of the method.
	 * @param name Name of the method.
	 * @param access Allowed access to the method.
	 * @param flags Method flags.
	 * @return The method builder.
	 * @throws IllegalArgumentException See {@link #addMethod(Method, Access)}.
	 */
	public Method.Instance addMethod(Type returnType, String name, Access access, MethodFlag... flags) {
		var method = Method.instanceMethod(returnType, name, type(), flags);
		addMethod(method, access);
		return method;
	}
	
	/**
	 * Creates and adds an abstract method to this class.
	 * @param returnType Return type of the method.
	 * @param name Name of the method.
	 * @param access Allowed access to the method.
	 * @param flags Method flags.
	 * @return The abstract method builder for adding arguments.
	 * @throws IllegalArgumentException See {@link #addMethod(Method, Access)}.
	 */
	public AbstractMethod addAbstractMethod(Type returnType, String name, Access access, MethodFlag... flags) {
		var method = Method.abstractMethod(returnType, name, flags);
		addMethod(method, access);
		return method;
	}
	
	/**
	 * Creates and adds a native method to this class.
	 * @param isStatic Whether or not this method is static.
	 * @param returnType Return type of the method.
	 * @param name Name of the method.
	 * @param access Allowed access to the method.
	 * @param flags Method flags.
	 * @return The abstract method builder for adding arguments.
	 */
	public AbstractMethod addNativeMethod(boolean isStatic, Type returnType, String name, Access access, MethodFlag... flags) {
		// TODO check validation for correct placement of native methods
		var method = Method.nativeMethod(isStatic, returnType, name, flags);
		addMethod(method, access);
		return method;
	}
	
	/**
	 * Creates and adds a static method to this class.
	 * @param returnType Return type of the method.
	 * @param name Name of the method.
	 * @param access Allowed access to the method.
	 * @param flags Method flags.
	 * @return The method builder.
	 * @throws IllegalArgumentException See {@link #addMethod(Method, Access)}.
	 */
	public Method.Static addStaticMethod(Type returnType, String name, Access access, MethodFlag... flags) {
		var method = Method.staticMethod(returnType, name, flags);
		addMethod(method, access);
		return method;
	}
	
	/**
	 * Creates and adds a constructor to this class.
	 * @param access Allowed access to the constructor.
	 * @param flags Method flags for the constructor.
	 * @return The method builder for constructor.
	 * @throws IllegalArgumentException See {@link #addMethod(Method, Access)}.
	 */
	public Method.Instance addConstructor(Access access, MethodFlag... flags) {
		return addMethod(Type.VOID, "<init>", access, flags);
	}
	
	public void addEmptyConstructor(Access access) {
		var superType = superClass != null ? superClass : Type.OBJECT;
		var constructor = addConstructor(access);
		constructor.add(constructor.self().callPrivate(superType, Type.VOID, "<init>"));
		constructor.add(Return.nothing());
	}
	
	/**
	 * Adds a new field to this class definition.
	 * @param isStatic Whether or not the field is static.
	 * @param access Access allowed to the field.
	 * @param type Type of the field.
	 * @param name Name of the field.
	 * @param flags Additional field flags, if any.
	 * @throws IllegalArgumentException If this class is an interface and the
	 * field is not {@link Access#PUBLIC public}, static and
	 * {@link FieldFlag#FINAL final}.
	 */
	public void addField(boolean isStatic, Access access, Type type, String name, FieldFlag... flags) {
		if ((this.access & Opcodes.ACC_INTERFACE) != 0) {
			var hasFinal = false;
			for (var flag : flags) {
				if (flag == FieldFlag.FINAL) {
					hasFinal = true;
				}
			}
			if (!isStatic || access != Access.PUBLIC || !hasFinal) {				
				throw new IllegalArgumentException("interfaces fields must be public, static and final");
			}
		}
		fields.add(new Field(isStatic, access, type, name, flags, null));
	}
	
	/**
	 * Adds a new non-static field to this class definition.
	 * @param access Access allowed to the field.
	 * @param type Type of the field.
	 * @param name Name of the field.
	 * @param flags Additional field flags, if any.
	 * @throws IllegalArgumentException If this class is an interface.
	 */
	public void addInstanceField(Access access, Type type, String name, FieldFlag... flags) {
		addField(false, access, type, name, flags);
	}
	
	/**
	 * Adds a new static field to this class definition.
	 * @param access Access allowed to the field.
	 * @param type Type of the field.
	 * @param name Name of the field.
	 * @param flags Additional field flags, if any.
	 * @throws IllegalArgumentException If this class is an interface and the
	 * field is not {@link Access#PUBLIC public} and {@link FieldFlag#FINAL final}.
	 */
	public void addStaticField(Access access, Type type, String name, FieldFlag... flags) {
		addField(true, access, type, name, flags);
	}
	
	/**
	 * Adds a new static field to this class definition.
	 * @param access Access allowed to the field.
	 * @param name Name of the field.
	 * @param initialValue Initial value of the field.
	 * @param flags Additional field flags, if any.
	 * @throws IllegalArgumentException If this class is an interface and the
	 * field is not {@link Access#PUBLIC public} and {@link FieldFlag#FINAL final}.
	 */
	public void addStaticField(Access access, String name, Constant initialValue, FieldFlag... flags) {
		fields.add(new Field(true, access, initialValue.type(), name, flags, initialValue));
	}
	
	public List<Method> methods() {
		return Collections.unmodifiableList(methods);
	}
	
	public byte[] compile(CompileOptions opts) {
		// Use COMPUTE_MAXS, because otherwise writing custom bytecode will
		// get ugly really fast
		// Frames we'll compute ourself
		var writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		// Enable ASM checks if user has requested them
		// But don't enable data flow checks, can't use them with COMPUTE_MAXS
		var cv = opts.asmChecks() ? new CheckClassAdapter(writer, false) : writer;
		var superName = superClass != null ? superClass.internalName() : "java/lang/Object";
		var interfaceNames = interfaces != null ? 
				Arrays.stream(interfaces).map(Type::internalName).toArray(String[]::new)
				: null;
		// TODO customizable Java version (needs more than just this flag, though)
		cv.visit(Opcodes.V17, access, name.replace('.', '/'), null, superName, interfaceNames);

		// Add fields
		for (var field : fields) {
			compileField(cv, field);
		}
		
		// Compile hooks may add methods, so we can't use iterator or foreach (ConcurrentModificationException)
		// Since nothing is ever removed from list, this should be safe
		var methodCompiler = new MethodCompiler(this, cv, opts);
		for (var i = 0; i < methods.size(); i++) {
			var method = methods.get(i);
			methodCompiler.compile(method, accessTable.get(method));
		}
		
		cv.visitEnd();
		return writer.toByteArray();
	}
	
	private void compileField(ClassVisitor cv, Field field) {
		// Compute access
		var access = field.access.value();
		if (field.isStatic()) {
			access |= Opcodes.ACC_STATIC;
		}
		for (var flag : field.flags()) {
			access |= flag.value();
		}
		
		// Emit field
		var initialValue = field.initialValue() != null ? field.initialValue().asmValue() : null;
		cv.visitField(access, field.name(), field.type().descriptor(), null, initialValue);
	}
	
	public byte[] compile() {
		return compile(CompileOptions.DEFAULT);
	}

}
