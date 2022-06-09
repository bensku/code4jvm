package fi.benjami.code4jvm.block;

import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Arrays;

import org.objectweb.asm.Handle;

import fi.benjami.code4jvm.ClassDef;
import fi.benjami.code4jvm.Expression;
import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.Value;
import fi.benjami.code4jvm.call.FixedCallTarget;
import fi.benjami.code4jvm.flag.Access;
import fi.benjami.code4jvm.flag.MethodFlag;
import fi.benjami.code4jvm.statement.Bytecode;
import fi.benjami.code4jvm.util.TypeUtils;

import static org.objectweb.asm.Opcodes.*;

/**
 * Lambda is a free function that can be:
 * <ul>
 * <li>Added to a class as static method
 * <li>Called directly from a {@link Block block}
 * <li>Used as implementation for sole method of a functional interface
 * </ul>
 * 
 * @see Method
 * 
 * @implNote All of above result in creation of a synthetic static method.
 *
 */
public class Lambda extends Routine {

	private static final Handle LAMBDA_METAFACTORY;
	
	static {
		try {
			LAMBDA_METAFACTORY = new Handle(H_INVOKESTATIC, "java/lang/invoke/LambdaMetafactory",
					"metafactory", org.objectweb.asm.Type.getMethodDescriptor(LambdaMetafactory.class.getDeclaredMethod("metafactory",
							MethodHandles.Lookup.class, String.class, MethodType.class, MethodType.class, MethodHandle.class, MethodType.class)),
					false);
		} catch (NoSuchMethodException | SecurityException e) {
			throw new AssertionError(e);
		}
	}
	
	public static record Shape(
			Type interfaceType,
			String methodName,
			Type returnType,
			Type... argTypes
	) {}
	
	public static Lambda create(Type returnType) {
		return new Lambda(Block.create(), returnType);
	}
	
	public static Shape shape(Type interfaceType, String methodName, Type returnType, Type... argTypes) {
		return new Shape(interfaceType, methodName, returnType, argTypes);
	}
	
	private FixedCallTarget backingMethod;
	
	private Lambda(Block block, Type returnType) {
		super(block, returnType);
	}
	
	private void setupHiddenMethod(ClassDef def) {
		if (backingMethod == null) {
			backingMethod = addStaticMethod(def);
		}
	}
	
	/**
	 * Calls this lambda directly.
	 * @param args Arguments for this lambda.
	 * @return An expression that represents the call.
	 * @implNote Results in creation of a synthetic static method in the class
	 * under which the expression is used.
	 */
	public Expression call(Value... args) {
		return block -> {
			block.setCompileHook(this, this::setupHiddenMethod);
			// Setup static call without StaticCallTarget, because we don't have the target up-front
			// (method is generated just before bytecode generation)
			return block.add(Bytecode.run(returnType(), args, ctx -> {
				ctx.asm().visitMethodInsn(INVOKESTATIC, backingMethod.owner().internalName(), backingMethod.name(),
						TypeUtils.methodDescriptor(returnType(), backingMethod.argTypes()), backingMethod.owner().isInterface());
			})).value();
		};
	}
	
	/**
	 * Creates a new instance of this lambda using the given
	 * functional interface.
	 * 
	 * <p>Note that JVM does not support creating lambda instances in hidden
	 * classes. Direct {@link #call(Value...) calls} work as expected.
	 * @param shape Functional interface shape.
	 * @return An expression that creates an object where this lambda implements
	 * the given functional interface.
	 * @implNote Like Java lambdas, this uses {@link LambdaMetafactory} with
	 * {@code invokedynamic}.
	 */
	public Expression newInstance(Shape shape, Value... capturedArgs) {
		return newInstance(shape.interfaceType(), shape.methodName(), capturedArgs);
	}
	
	/**
	 * Creates a new instance of this lambda using the given
	 * functional interface.
	 * 
	 * <p>Note that JVM does not support creating lambda instances in hidden
	 * classes. Direct {@link #call(Value...) calls} work as expected with them.
	 * @param interfaceType Functional interface type.
	 * @param methodName Name of the implemented method in interface.
	 * @param capturedArgs Arguments that are captured where the returned
	 * expression is added to a block, and missing from the functional
	 * interface.
	 * @return An expression that creates an object where this lambda implements
	 * the given functional interface.
	 * @implNote Like Java lambdas, this uses {@link LambdaMetafactory} with
	 * {@code invokedynamic}.
	 */
	public Expression newInstance(Type interfaceType, String methodName, Value... capturedArgs) {
		return block -> {
			block.setCompileHook(this, this::setupHiddenMethod);
			return block.add(Bytecode.run(interfaceType, capturedArgs, ctx -> {
				// Set up invokedynamic call to method that creates instance of the lambda
				// with LambdaMetafactory#metafactory as bootstrap
				// Captured values are given as arguments
				var capturedTypes = Arrays.copyOf(backingMethod.argTypes(), capturedArgs.length);
				var argTypes = Arrays.copyOfRange(backingMethod.argTypes(), capturedArgs.length, backingMethod.argTypes().length);
				var interfaceMethodType = org.objectweb.asm.Type.getMethodType(TypeUtils.methodDescriptor(returnType(), argTypes));
				var bootstrapArgs = new Object[] {
						interfaceMethodType,
						backingMethod.toMethodHandle(), // implementation
						interfaceMethodType // dynamicMethodType
				};
				ctx.asm().visitInvokeDynamicInsn(methodName,
						TypeUtils.methodDescriptor(interfaceType, capturedTypes),
						LAMBDA_METAFACTORY,
						bootstrapArgs);
			})).value();
		};
	}
	
	private String lambdaName(ClassDef def) {
		return "lambda$" + def.methods().size();
	}
	
	/**
	 * Converts this lambda to a static method.
	 * @param name Method name.
	 * @param flags Method flags.
	 * @return Static method builder.
	 */
	public Method.Static asStaticMethod(String name, MethodFlag... flags) {
		var method = new Method.Static(block(), returnType(), name, flags);
		method.args.addAll(args);
		return method;
	}
	
	/**
	 * Converts this lambda to an instance method.
	 * @param parentClass Type of {@code this} value, i.e. the first
	 * {@link #arg(Type) argument} received by this lambda.
	 * @param name Method name.
	 * @param flags Method flags.
	 * @return Instance method builder.
	 */
	public Method.Instance asInstanceMethod(Type parentClass, String name, MethodFlag... flags) {
		var method = new Method.Instance(block(), returnType(), name, parentClass, flags);
		// First slot is reserved for this/self(), method doesn't have an argument for it
		method.args.addAll(args.subList(1, args.size()));
		return method;
	}
	
	/**
	 * Adds a synthetic static method with body of this lambda to a class.
	 * @param def Class builder.
	 * @return Call target for the added method.
	 */
	public FixedCallTarget addStaticMethod(ClassDef def) {
		var name = lambdaName(def);
		def.addMethod(asStaticMethod(name, MethodFlag.SYNTHETIC), Access.PUBLIC);
		return def.type().staticMethod(returnType(), name,
				args.stream().map(Value::type).toArray(Type[]::new));
	}
	
	/**
	 * Adds a synthetic instance method with body of this lambda to a class.
	 * @param def Class builder.
	 * @return Call target for the added method.
	 */
	public FixedCallTarget addInstanceMethod(ClassDef def) {
		var name = lambdaName(def);
		def.addMethod(asInstanceMethod(def.type(), name, MethodFlag.SYNTHETIC), Access.PUBLIC);
		return def.type().virtualMethod(returnType(), name,
				args.stream().map(Value::type).toArray(Type[]::new));
	}
	
}
