package fi.benjami.code4jvm;

import java.util.Arrays;
import java.util.Optional;

import fi.benjami.code4jvm.block.Block;
import fi.benjami.code4jvm.call.FixedCallTarget;
import fi.benjami.code4jvm.internal.CastValue;
import fi.benjami.code4jvm.internal.LocalVar;
import fi.benjami.code4jvm.statement.Bytecode;

import static org.objectweb.asm.Opcodes.*;

public interface Value {
	
	static Expression uninitialized(Type type) {
		return block -> {
			return block.add(Bytecode.stub(type, new Value[] {LocalVar.EMPTY_MARKER})).value();
		};
	}

	Type type();
	
	Optional<Block> parentBlock();
	
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
			return block.add(Bytecode.stub(type(), new Value[] {this})).value();
		};
	}
	
	default Expression getField(Type fieldType, String name) {
		return block -> {
			return block.add(Bytecode.run(fieldType, new Value[] {this}, ctx -> {
				ctx.asm().visitFieldInsn(GETFIELD, type().internalName(), name, fieldType.descriptor());
			})).value();
		};
	}
	
	default Statement putField(Type fieldType, String name, Value value) {
		return block -> {
			block.add(Bytecode.run(Type.VOID, new Value[] {this, value}, ctx -> {
				ctx.asm().visitFieldInsn(PUTFIELD, type().internalName(), name, fieldType.descriptor());
			}));
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
