package fi.benjami.code4jvm.call;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import fi.benjami.code4jvm.Expression;
import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.Value;
import fi.benjami.code4jvm.statement.Bytecode;
import fi.benjami.code4jvm.util.TypeUtils;

import static org.objectweb.asm.Opcodes.*;

public class InitCallTarget extends CallTarget {

	public InitCallTarget(Type owner, Type[] argTypes) {
		super(owner, false, owner, "<init>", argTypes);
	}

	@Override
	public Expression call(Value... args) {
		return block -> {
			var ownerName = owner().internalName();
			
			// Create new object and leave two references of it to stack
			var instance = block.add(Bytecode.run(returnType(), List.of(), mv -> {
				mv.visitTypeInsn(NEW, ownerName);
				mv.visitInsn(DUP);
			})).value();
			
			// Load arguments to stack on top of them
			var inputs = new ArrayList<Value>(args.length + 1);
			inputs.add(instance); // Already on stack
			inputs.addAll(Arrays.asList(args));
			
			// Call constructor to consume one reference to object and all arguments
			block.add(Bytecode.run(returnType(), inputs, mv -> {
				mv.visitMethodInsn(INVOKESPECIAL, ownerName, "<init>",
						TypeUtils.methodDescriptor(Type.VOID, argTypes()), false);
			}));
			
			// Return the other reference to the object, now initialized
			return instance;
		};
	}

}
