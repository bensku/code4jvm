package fi.benjami.code4jvm;


import java.util.Arrays;

import fi.benjami.code4jvm.call.CallTarget;
import fi.benjami.code4jvm.call.FixedCallTarget;
import fi.benjami.code4jvm.statement.Bytecode;

import static org.objectweb.asm.Opcodes.*;

public class Type {
	
	private record TypeOpcodes(
			int load,
			int store,
			int aload,
			int astore,
			int add,
			int sub,
			int mul,
			int div,
			int rem,
			int neg,
			int shl,
			int shr,
			int ushr,
			int and,
			int or,
			int xor,
			int return_
	) {}

	public static final Type VOID = new Type("void", "V",
			new TypeOpcodes(NOP, NOP, NOP, NOP, NOP, NOP, NOP, NOP, NOP, NOP, NOP, NOP, NOP, NOP, NOP, NOP, RETURN));
	public static final Type BOOLEAN = new Type("boolean", "Z",
			new TypeOpcodes(ILOAD, ISTORE, BALOAD, BASTORE, IADD, ISUB, IMUL, IDIV, IREM, INEG, ISHL, ISHR, IUSHR, IAND, IOR, IXOR, IRETURN));
	public static final Type BYTE = new Type("byte", "B",
			new TypeOpcodes(ILOAD, ISTORE, BALOAD, BASTORE, IADD, ISUB, IMUL, IDIV, IREM, INEG, ISHL, ISHR, IUSHR, IAND, IOR, IXOR, IRETURN));
	public static final Type SHORT = new Type("short", "S",
			new TypeOpcodes(ILOAD, ISTORE, SALOAD, SASTORE, IADD, ISUB, IMUL, IDIV, IREM, INEG, ISHL, ISHR, IUSHR, IAND, IOR, IXOR, IRETURN));
	public static final Type CHAR = new Type("char", "C",
			new TypeOpcodes(ILOAD, ISTORE, CALOAD, CASTORE, IADD, ISUB, IMUL, IDIV, IREM, INEG, ISHL, ISHR, IUSHR, IAND, IOR, IXOR, IRETURN));
	public static final Type INT = new Type("int", "I",
			new TypeOpcodes(ILOAD, ISTORE, IALOAD, IASTORE, IADD, ISUB, IMUL, IDIV, IREM, INEG, ISHL, ISHR, IUSHR, IAND, IOR, IXOR, IRETURN));
	public static final Type LONG = new Type("long", "J",
			new TypeOpcodes(LLOAD, LSTORE, LALOAD, LASTORE, LADD, LSUB, LMUL, LDIV, LREM, LNEG, LSHL, LSHR, LUSHR, LAND, LOR, LXOR, LRETURN));
	public static final Type FLOAT = new Type("float", "F",
			new TypeOpcodes(FLOAD, FSTORE, FALOAD, FASTORE, FADD, FSUB, FMUL, FDIV, FREM, FNEG, NOP, NOP, NOP, NOP, NOP, NOP, FRETURN));
	public static final Type DOUBLE = new Type("double", "D",
			new TypeOpcodes(DLOAD, DSTORE, DALOAD, DASTORE, DADD, DSUB, DMUL, DDIV, DREM, DNEG, NOP, NOP, NOP, NOP, NOP, NOP, DRETURN));
	
	private static final int KIND_PRIMITIVE = 0, KIND_CLASS = 1, KIND_INTERFACE = 2;
	
	// All objects work with same opcodes
	private static final TypeOpcodes OBJ_OPCODES =
			new TypeOpcodes(ALOAD, ASTORE, AALOAD, AASTORE, NOP, NOP, NOP, NOP, NOP, NOP, NOP, NOP, NOP, NOP, NOP, NOP, ARETURN);
	
	// j.l.Object is used so often that a constant is nice to have
	public static final Type OBJECT = new Type("java.lang.Object", "java/lang/Object", KIND_CLASS, "Ljava/lang/Object;", 0, OBJ_OPCODES);
	

	public static Type of(String name, boolean isInterface) {
		return switch (name) {
		case "boolean" -> BOOLEAN;
		case "byte" -> BYTE;
		case "short" -> SHORT;
		case "char" -> CHAR;
		case "int" -> INT;
		case "long" -> LONG;
		case "float" -> FLOAT;
		case "double" -> DOUBLE;
		case "java.lang.Object" -> OBJECT;
		default -> {
			var internalName = name.replace('.', '/');
			yield new Type(name, internalName, isInterface ? KIND_INTERFACE : KIND_CLASS,
					"L" + internalName + ";", 0, OBJ_OPCODES);
		}
		};
	}
	
	public static Type of(Class<?> c) {
		if (c == boolean.class) {
			return BOOLEAN;
		} else if (c == byte.class) {
			return BYTE;
		} else if (c == short.class) {
			return SHORT;
		} else if (c == char.class) {
			return CHAR;
		} else if (c == int.class) {
			return INT;
		} else if (c == long.class) {
			return LONG;
		} else if (c == float.class) {
			return FLOAT;
		} else if (c == double.class) {
			return DOUBLE;
		} else if (c == Object.class) {
			return OBJECT;
		} else {
			var name = c.getName();
			var internalName = name.replace('.', '/');
			var kind = c.isInterface() ? KIND_INTERFACE : KIND_CLASS;
			return new Type(name, internalName, kind, "L" + internalName + ";", 0, OBJ_OPCODES);
		}
	}
	
	/**
	 * Java type name.
	 */
	private final String name;
	
	/**
	 * Internal name (JVM type name).
	 */
	private final String internalName;
	
	private final int kind;

	/**
	 * Type descriptor.
	 */
	private final String descriptor;
	
	/**
	 * Array dimensions of the type. 0 if not array.
	 */
	private final int arrayDimensions;
	
	private final TypeOpcodes opcodes;
	
	private Type(String name, String internalName, int kind, String descriptor, int arrayDimensions,
			TypeOpcodes opcodes) {
		this.name = name;
		this.internalName = internalName;
		this.kind = kind;
		this.descriptor = descriptor;
		this.arrayDimensions = arrayDimensions;
		this.opcodes = opcodes;
	}
	
	// For primitive types only
	private Type(String name, String descriptor, TypeOpcodes opcodes) {
		this(name, name, KIND_PRIMITIVE, descriptor, 0, opcodes);
	}
	
	public String name() {
		return name;
	}
	
	public String internalName() {
		return internalName;
	}
	
	public String descriptor() {
		return descriptor;
	}
	
	public int arrayDimensions() {
		return arrayDimensions;
	}
	
	public boolean isArray() {
		return arrayDimensions != 0;
	}
	
	public Type array(int dimensions) {
		// TODO array opcodes!
		return new Type(name, internalName, kind, "[".repeat(dimensions) + descriptor, dimensions, null);
	}
	
	public boolean isPrimitive() {
		return kind == KIND_PRIMITIVE;
	}
	
	public boolean isObject() {
		return !isPrimitive();
	}
	
	public boolean isInterface() {
		// Arrays of interface types are technically not interfaces
		return kind == KIND_INTERFACE && arrayDimensions == 0;
	}
	
	public FixedCallTarget constructor(Type... argTypes) {
		return CallTarget.constructor(this, argTypes);
	}
	
	public Expression newInstance(Value... args) {
		return constructor(Arrays.stream(args).map(Value::type).toArray(Type[]::new))
				.call(args);
	}
	
	// Method calls
	
	public FixedCallTarget staticMethod(Type returnType, String name, Type... argTypes) {
		return CallTarget.staticMethod(this, returnType, name, argTypes);
	}
	
	public Expression callStatic(Type returnType, String name, Value... args) {
		return staticMethod(returnType, name, Arrays.stream(args).map(Value::type).toArray(Type[]::new))
				.call(args);
	}
	
	public FixedCallTarget virtualMethod(Type returnType, String name, Type... argTypes) {
		return CallTarget.virtualMethod(this, returnType, name, argTypes);
	}
	
	public Expression callVirtual(Type returnType, String name, Value... args) {
		return virtualMethod(returnType, name, Arrays.stream(args).map(Value::type).toArray(Type[]::new))
				.call(args);
	}
	
	public FixedCallTarget privateMethod(Type returnType, String name, Type... argTypes) {
		return CallTarget.privateMethod(this, returnType, name, argTypes);
	}
	
	public Expression callPrivate(Type returnType, String name, Value... args) {
		return privateMethod(returnType, name, Arrays.stream(args).map(Value::type).toArray(Type[]::new))
				.call(args);
	}
	
	// Field access
	
	public Expression getStatic(Type fieldType, String name) {
		return block -> {
			return block.add(Bytecode.run(fieldType, new Value[0], mv -> {
				mv.visitFieldInsn(GETSTATIC, internalName(), name, fieldType.descriptor());
			})).value();
		};
	}
	
	public Statement putStatic(Type fieldType, String name, Value value) {
		return block -> {
			block.add(Bytecode.run(Type.VOID, new Value[] {value}, mv -> {
				mv.visitFieldInsn(PUTSTATIC, internalName(), name, fieldType.descriptor());
			}));
		};
	}
	
	public int getOpcode(int intOpcode) {
		var opcode = switch (intOpcode) {
		case ILOAD -> opcodes.load;
		case ISTORE -> opcodes.store;
		case IALOAD -> opcodes.aload;
		case IASTORE -> opcodes.astore;
		case IADD -> opcodes.add;
		case ISUB -> opcodes.sub;
		case IMUL -> opcodes.mul;
		case IDIV -> opcodes.div;
		case IREM -> opcodes.rem;
		case INEG -> opcodes.neg;
		case ISHL -> opcodes.shl;
		case ISHR -> opcodes.shr;
		case IUSHR -> opcodes.ushr;
		case IAND -> opcodes.and;
		case IOR -> opcodes.or;
		case IXOR -> opcodes.xor;
		case IRETURN -> opcodes.return_;
		default -> intOpcode;
		};
		if (opcode == NOP) {
			throw new IllegalArgumentException("opcode " + intOpcode + " is not supported for " + this);
		}
		return opcode;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof Type o) {
			// Descriptor encodes name and array dimensions
			return descriptor.equals(o.descriptor);
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return descriptor.hashCode();
	}
	
	@Override
	public String toString() {
		return descriptor; // TODO more developer-friendly string?
	}
}
