package fi.benjami.code4jvm.call;

import java.util.Arrays;

import fi.benjami.code4jvm.Expression;
import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.Value;
import fi.benjami.code4jvm.statement.Bytecode;
import fi.benjami.code4jvm.util.TypeUtils;

import static org.objectweb.asm.Opcodes.*;

public class StaticCallTarget extends CallTarget {

	public StaticCallTarget(Type owner, boolean ownerIsInterface, Type returnType, String name, Type[] argTypes) {
		super(owner, ownerIsInterface, returnType, name, argTypes);
	}

	@Override
	public Expression call(Value... args) {
		return block -> {
			return block.add(Bytecode.run(returnType(), Arrays.asList(args), mv -> {
				mv.visitMethodInsn(INVOKESTATIC, owner().internalName(), name(),
						TypeUtils.methodDescriptor(returnType(), argTypes()), ownerIsInterface());
			})).value();
		};
	}

}
