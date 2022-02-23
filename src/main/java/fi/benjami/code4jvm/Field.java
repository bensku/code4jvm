package fi.benjami.code4jvm;

import java.util.List;

import org.objectweb.asm.Type;

import fi.benjami.code4jvm.statement.Bytecode;

import static org.objectweb.asm.Opcodes.*;

public class Field {
	
	public static Expression getStatic(Type definingClass, Type fieldType, String name) {
		return block -> {
			return block.add(Bytecode.run(fieldType, List.of(), mv -> {
				mv.visitFieldInsn(GETSTATIC, definingClass.getInternalName(), name, fieldType.getDescriptor());
			})).value();
		};
	}
	
	public static Statement putStatic(Type definingClass, Type fieldType, String name, Value value) {
		return block -> {
			block.add(Bytecode.run(Type.VOID_TYPE, List.of(value), mv -> {
				mv.visitFieldInsn(PUTSTATIC, definingClass.getInternalName(), name, fieldType.getDescriptor());
			}));
		};
	}

	private final Type definingClass;
	private final Type type;
	private final String name;
	
	Field(Type definingClass, Type type, String name) {
		this.definingClass = definingClass;
		this.type = type;
		this.name = name;
	}
	
	public Type definingClass() {
		return definingClass;
	}
	
	public Type type() {
		return type;
	}
	
	public String name() {
		return name;
	}
}
