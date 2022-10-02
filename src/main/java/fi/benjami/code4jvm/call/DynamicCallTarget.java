package fi.benjami.code4jvm.call;

import java.util.Arrays;

import fi.benjami.code4jvm.Constant;
import fi.benjami.code4jvm.Expression;
import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.Value;
import fi.benjami.code4jvm.statement.Bytecode;
import fi.benjami.code4jvm.util.TypeUtils;

/**
 * A call target which is linked runtime to whatever the
 * {@link #bootstrapMethod() bootstrap method} returns.
 * 
 * @implNote Compiles to {@code invokedynamic}.
 *
 */
public final class DynamicCallTarget extends CallTarget {

	private final FixedCallTarget bootstrapMethod;
	
	DynamicCallTarget(Type returnType, String name, Type[] argTypes, Value[] capturedArgs, FixedCallTarget bootstrapMethod) {
		super(returnType, name, argTypes, Linkage.DYNAMIC, capturedArgs);
		this.bootstrapMethod = bootstrapMethod;
	}
	
	public FixedCallTarget bootstrapMethod() {
		return bootstrapMethod;
	}
	
	@Override
	public DynamicCallTarget withCapturedArgs(Value... args) {
		return new DynamicCallTarget(returnType(), name(), argTypes(),
				CallTarget.mergeArgs(capturedArgs(), args), bootstrapMethod);
	}
	
	@Override
	public DynamicCallTarget withoutCapturedArgs() {
		return new DynamicCallTarget(returnType(), name(), argTypes(), new Value[0], bootstrapMethod);
	}

	@Override
	public Expression call(Value... args) {
		var allArgs = CallTarget.mergeArgs(capturedArgs(), args);
		var argTypes = Arrays.stream(allArgs).map(Value::type).toArray(Type[]::new);
		return block -> {
			return block.add(Bytecode.run(returnType(), allArgs, ctx -> {
				// Bootstrap arguments are just arguments the bootstrapMethod has captured!
				var bootstrapArgs = Arrays.stream(bootstrapMethod.capturedArgs())
						.map(val -> ((Constant) val).asmValue())
						.toArray();
				
				ctx.asm().visitInvokeDynamicInsn(name(),
						TypeUtils.methodDescriptor(returnType(), argTypes),
						bootstrapMethod.toMethodHandle(),
						bootstrapArgs);
			}, Bytecode.name("call dynamic, bootstrap %s", bootstrapMethod))).value();
		};
	}

}
