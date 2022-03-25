package fi.benjami.code4jvm;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import fi.benjami.code4jvm.call.InstanceCallTarget;
import fi.benjami.code4jvm.call.Linkage;
import fi.benjami.code4jvm.call.MethodLookup;
import fi.benjami.code4jvm.internal.CastValue;
import fi.benjami.code4jvm.internal.LocalVar;
import fi.benjami.code4jvm.statement.Bytecode;

import static org.objectweb.asm.Opcodes.*;

public interface Value {
	
	static Expression uninitialized(Type type) {
		return block -> {
			return block.add(Bytecode.stub(type, List.of(LocalVar.EMPTY_MARKER))).value();
		};
	}

	Type type();
	
	Optional<Block> parentBlock();
	
	Optional<String> name();
	
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
			return block.add(Bytecode.stub(type(), List.of(this))).value();
		};
	}
	
	default Expression getField(Type fieldType, String name) {
		return block -> {
			return block.add(Bytecode.run(fieldType, List.of(this), mv -> {
				mv.visitFieldInsn(GETFIELD, type().internalName(), name, fieldType.descriptor());
			})).value();
		};
	}
	
	default Statement putField(Type fieldType, String name, Value value) {
		return block -> {
			block.add(Bytecode.run(Type.VOID, List.of(this, value), mv -> {
				mv.visitFieldInsn(PUTFIELD, type().internalName(), name, fieldType.descriptor());
			}));
		};
	}
	
	default InstanceCallTarget findVirtual(Type returnType, String name, Type... argTypes) {
		return new InstanceCallTarget(type(), this, type().isInterface(), Linkage.VIRTUAL, returnType, name, argTypes);
	}
	
	default Expression callVirtual(Type returnType, String name, Value... args) {
		return findVirtual(returnType, name, Arrays.stream(args).map(Value::type).toArray(Type[]::new))
				.call(args);
	}
	
	default InstanceCallTarget findSpecial(Type owner, Type returnType, String name, Type... argTypes) {
		return new InstanceCallTarget(owner, this, owner.isInterface(), Linkage.SPECIAL, returnType, name, argTypes);
	}
	
	default InstanceCallTarget findSpecial(Type returnType, String name, Type... argTypes) {
		return findSpecial(type(), returnType, name, argTypes);
	}
	
	default Expression callSpecial(Type owner, Type returnType, String name, Value... args) {
		return findSpecial(owner, returnType, name, Arrays.stream(args).map(Value::type).toArray(Type[]::new))
				.call(args);
	}
	
	default Expression callSpecial(Type returnType, String name, Value... args) {
		return callSpecial(type(), returnType, name, args);
	}
	
}
