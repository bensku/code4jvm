package fi.benjami.code4jvm.block;

import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Arrays;
import java.util.List;

import org.objectweb.asm.Handle;

import fi.benjami.code4jvm.ClassDef;
import fi.benjami.code4jvm.Expression;
import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.Value;
import fi.benjami.code4jvm.call.StaticCallTarget;
import fi.benjami.code4jvm.flag.Access;
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
	
	private StaticCallTarget backingMethod;
	
	private Lambda(Block block, Type returnType) {
		super(block, returnType);
	}
	
	private void setupHiddenMethod(ClassDef def) {
		backingMethod = toStaticMethod(def);
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
			return block.add(Bytecode.run(returnType(), Arrays.asList(args), mv -> {
				mv.visitMethodInsn(INVOKESTATIC, backingMethod.owner().internalName(), backingMethod.name(),
						TypeUtils.methodDescriptor(returnType(), backingMethod.argTypes()), backingMethod.ownerIsInterface());
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
	 * classes. Direct {@link #call(Value...) calls} work as expected.
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
			return block.add(Bytecode.run(interfaceType, List.of(capturedArgs), mv -> {
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
				mv.visitInvokeDynamicInsn(methodName,
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
	 * Adds this lambda as a static method to given class definition.
	 * @param def Class definition.
	 * @return Call target of the generated method.
	 */
	public StaticCallTarget toStaticMethod(ClassDef def) {
		var method = new Method.Static(block(), returnType(), lambdaName(def), ACC_SYNTHETIC);
		method.args.addAll(args);
		def.addMethod(method, Access.PUBLIC);
		return def.type().findStatic(returnType(), method.name(),
				args.stream().map(Value::type).toArray(Type[]::new));
	}
	
	// TODO toInstanceMethod - InstanceCallTarget expects this Value which we don't have here
}
