package fi.benjami.code4jvm;

import java.util.List;
import java.util.Optional;

import org.objectweb.asm.Type;

import fi.benjami.code4jvm.call.InstanceCallTarget;
import fi.benjami.code4jvm.call.Linkage;
import fi.benjami.code4jvm.call.MethodLookup;
import fi.benjami.code4jvm.internal.CastValue;
import fi.benjami.code4jvm.statement.Bytecode;

import static org.objectweb.asm.Opcodes.*;

public interface Value {

	Type type();
	
	Optional<Block> parentBlock();
	
	Optional<String> name();
	
	default Value cast(Type to) {
		return CastValue.cast(this, to);
	}
	
	default Value asType(Type to) {
		return CastValue.fakeCast(null, to);
	}
	
	default Expression copy() {
		return block -> {
			// Load to stack and keep it there
			// Block and Bytecode handle slot allocation
			return block.add(Bytecode.run(type(), List.of(this), mv -> {})).value();
		};
	}
	
	default Expression getField(Type fieldType, String name) {
		return block -> {
			return block.add(Bytecode.run(fieldType, List.of(this), mv -> {
				mv.visitFieldInsn(GETFIELD, type().getInternalName(), name, fieldType.getDescriptor());
			})).value();
		};
	}
	
	default Statement putField(Type fieldType, String name, Value value) {
		return block -> {
			block.add(Bytecode.run(Type.VOID_TYPE, List.of(this, value), mv -> {
				mv.visitFieldInsn(PUTFIELD, type().getInternalName(), name, fieldType.getDescriptor());
			}));
		};
	}
	
	default MethodLookup<InstanceCallTarget> virtualLookup(boolean ownerIsInterface) {
		return MethodLookup.ofInstance(type(), this, ownerIsInterface, Linkage.VIRTUAL);
	}
	
	default MethodLookup<InstanceCallTarget> specialLookup(Type owner, boolean ownerIsInterface) {
		return MethodLookup.ofInstance(owner, this, ownerIsInterface, Linkage.SPECIAL);
	}
	
	default MethodLookup<InstanceCallTarget> specialLookup(boolean ownerIsInterface) {
		return specialLookup(type(), ownerIsInterface);
	}
	
}
