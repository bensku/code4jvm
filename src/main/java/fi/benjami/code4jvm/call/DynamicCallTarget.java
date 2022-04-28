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
public class DynamicCallTarget extends CallTarget {

	private final StaticCallTarget bootstrapMethod;
	private final Constant[] bootstrapArgs;
	
	/**
	 * Creates a new dynamic call target.
	 * @param name Method name. This is given as argument to bootstrap method runtime.
	 * @param bootstrap Bootstrap method.
	 * @param bootstrapArgs Additional arguments for the bootstrap method.
	 */
	public DynamicCallTarget(String name, StaticCallTarget bootstrap, Constant... bootstrapArgs) {
		super(name);
		this.bootstrapMethod = bootstrap;
		this.bootstrapArgs = bootstrapArgs;
	}
	
	public StaticCallTarget bootstrapMethod() {
		return bootstrapMethod;
	}
	
	public Constant[] bootstrapArgs() {
		return bootstrapArgs;
	}

	@Override
	public Expression call(Type returnType, Value... args) {
		var argTypes = Arrays.stream(args).map(Value::type).toArray(Type[]::new);
		return block -> {
			return block.add(Bytecode.run(returnType, Arrays.asList(args), mv -> {
				mv.visitInvokeDynamicInsn(name(),
						TypeUtils.methodDescriptor(returnType, argTypes),
						bootstrapMethod.asMethodHandle(),
						Arrays.stream(bootstrapArgs).map(Constant::asmValue).toArray());
			})).value();
		};
	}

}
