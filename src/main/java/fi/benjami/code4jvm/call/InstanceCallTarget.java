package fi.benjami.code4jvm.call;

import java.util.ArrayList;
import java.util.Arrays;

import org.objectweb.asm.Handle;

import fi.benjami.code4jvm.Expression;
import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.Value;
import fi.benjami.code4jvm.statement.Bytecode;
import fi.benjami.code4jvm.util.TypeUtils;

import static org.objectweb.asm.Opcodes.*;

/**
 * Call target that is an instance method.
 * 
 * @implNote Compiles to {@code invokevirtual} or {@code invokeinterface}.
 *
 */
public class InstanceCallTarget extends FixedCallTarget {
	
	private final Value instance;
	private final boolean ownerIsInterface;
	private final Linkage linkage;

	public InstanceCallTarget(Type owner, Value instance, boolean ownerIsInterface, Linkage linkage, Type returnType, String name, Type[] argTypes) {
		super(owner, ownerIsInterface, returnType, name, argTypes);
		this.instance = instance;
		this.ownerIsInterface = ownerIsInterface;
		this.linkage = linkage;
	}
	
	public Value instance() {
		return instance;
	}
	
	public boolean ownerIsInterface() {
		return ownerIsInterface;
	}
	
	public Linkage linkage() {
		return linkage;
	}

	@Override
	public Expression call(Value... args) {
		// TODO optionally validate argument types
		
		// Select the correct method call opcode based on lookup information
		int opcode = getOpcode();
		
		// Inputs are the instance, followed by all arguments
		var inputs = new ArrayList<Value>();
		inputs.add(instance);
		inputs.addAll(Arrays.asList(args));
		
		return block -> {
			return block.add(Bytecode.run(returnType(), inputs, mv -> {
				mv.visitMethodInsn(opcode, owner().internalName(), name(),
						TypeUtils.methodDescriptor(returnType(), argTypes()), ownerIsInterface);
			})).value();
		};
	}
	
	private int getOpcode() {
		if (linkage == Linkage.SPECIAL) {
			return INVOKESPECIAL;
		} else {
			return ownerIsInterface ? INVOKEINTERFACE : INVOKEVIRTUAL;
		}
	}

	@Override
	public Handle toMethodHandle() {
		return new Handle(getOpcode(), owner().internalName(), name(),
				TypeUtils.methodDescriptor(returnType(), argTypes()), ownerIsInterface());
	}

}
