package fi.benjami.code4jvm;

import java.util.Arrays;
import java.util.Optional;

import fi.benjami.code4jvm.call.FixedCallTarget;
import fi.benjami.code4jvm.internal.CastValue;
import fi.benjami.code4jvm.internal.StackTop;
import fi.benjami.code4jvm.statement.ArrayAccess;
import fi.benjami.code4jvm.statement.Bytecode;

import static org.objectweb.asm.Opcodes.*;

public interface Value {
	
	/**
	 * Creates a value that represents what is already available on stack.
	 * This is necessary for accessing values that are stacked by JVM, e.g.
	 * when handling exceptions by hand.
	 * 
	 * <p>This is a low-level utility. Unless you're generating
	 * {@link Bytecode custom bytecode}, you probably won't need this.
	 * @param type Type of value that is already on stack.
	 * @return Value that is already on stack.
	 */
	public static Value stackTop(Type type) {
		return new StackTop(type);
	}

	Type type();
	
	Optional<String> name();
	
	Value original();
	
	default Value cast(Type to) {
		return CastValue.cast(this, to);
	}
	
	default Value asType(Type to) {
		return CastValue.fakeCast(this, to);
	}
	
	default Expression copy() {
		return block -> {
			// Load to stack and keep it there
			// Block and Bytecode handle slot allocation
			return block.add(Bytecode.stub(type(), new Value[] {this}));
		};
	}
	
	default Expression getField(Type fieldType, String name) {
		if (type().isArray()) {
			if (!fieldType.equals(Type.INT)) {
				throw new IllegalArgumentException("array length is an int");
			}
			if (name.equals("length")) {
				// From Java's perspective, length is almost a field
				// This is not the case for JVM, but we can make it look like that
				return ArrayAccess.length(this);
			} else {
				throw new IllegalArgumentException("arrays have no fields except the length");
			}
		}
		return block -> {
			return block.add(Bytecode.run(fieldType, new Value[] {this}, ctx -> {
				ctx.asm().visitFieldInsn(GETFIELD, type().internalName(), name, fieldType.descriptor());
			}, Bytecode.name("get field %s.%s", type(), name)));
		};
	}
	
	default Statement putField(String name, Value value) {
		if (type().isArray()) {
			throw new IllegalStateException("arrays have no writable fields");
		}
		return block -> {
			block.add(Bytecode.run(Type.VOID, new Value[] {this, value}, ctx -> {
				ctx.asm().visitFieldInsn(PUTFIELD, type().internalName(), name, value.type().descriptor());
			}, Bytecode.name("put field %s.%s", type(), name)));
		};
	}
	
	default FixedCallTarget virtualMethod(Type returnType, String name, Type... argTypes) {
		return type().virtualMethod(returnType, name, argTypes).withCapturedArgs(this);
	}
	
	default Expression callVirtual(Type returnType, String name, Value... args) {
		return virtualMethod(returnType, name, Arrays.stream(args).map(Value::type).toArray(Type[]::new))
				.call(args);
	}
	
	default FixedCallTarget privateMethod(Type owner, Type returnType, String name, Type... argTypes) {
		return owner.privateMethod(returnType, name, argTypes).withCapturedArgs(this);
	}
	
	default FixedCallTarget privateMethod(Type returnType, String name, Type... argTypes) {
		return type().privateMethod(returnType, name, argTypes).withCapturedArgs(this);
	}
	
	default Expression callPrivate(Type owner, Type returnType, String name, Value... args) {
		return privateMethod(owner, returnType, name, Arrays.stream(args).map(Value::type).toArray(Type[]::new))
				.call(args);
	}
	
	default Expression callPrivate(Type returnType, String name, Value... args) {
		return callPrivate(type(), returnType, name, args);
	}
	
}
