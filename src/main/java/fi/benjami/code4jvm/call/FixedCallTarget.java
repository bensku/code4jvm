package fi.benjami.code4jvm.call;

import static org.objectweb.asm.Opcodes.*;

import org.objectweb.asm.Handle;

import fi.benjami.code4jvm.Expression;
import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.Value;
import fi.benjami.code4jvm.statement.Bytecode;
import fi.benjami.code4jvm.util.TypeUtils;

/**
 * A call target that is linked load-time to a fixed method.
 * 
 * @implNote Compiles to {@code invokestatic}, {@code invokevirtual} or
 * {@code invokespecial}, depending on {@link #linkage() linkage} used.
 *
 */
public final class FixedCallTarget extends CallTarget {
	
	private final Type owner;
	
	FixedCallTarget(Type returnType, String name, Type[] argTypes, Linkage linkage, Value[] capturedArgs, Type owner) {
		super(returnType, name, argTypes, linkage, capturedArgs);
		this.owner = owner;
	}
	
	public Type owner() {
		return owner;
	}
	
	@Override
	public FixedCallTarget withCapturedArgs(Value... args) {
		return new FixedCallTarget(returnType(), name(), argTypes(), linkage(),
				CallTarget.mergeArgs(capturedArgs(), args), owner);
	}
	
	@Override
	public FixedCallTarget withoutCapturedArgs() {
		return new FixedCallTarget(returnType(), name(), argTypes(), linkage(), new Value[0], owner);
	}
	
	@Override
	public Expression call(Value... args) {
		// Concatenate captured and other arguments
		var allArgs = CallTarget.mergeArgs(capturedArgs(), args);
		
		return block -> {
			return switch (linkage()) {
			case STATIC -> block.add(Bytecode.run(returnType(), allArgs, ctx -> {
					ctx.asm().visitMethodInsn(INVOKESTATIC, owner.internalName(), name(),
							TypeUtils.methodDescriptor(returnType(), argTypes()), owner.isInterface());
				})).value();
			// TODO first arg is this, take it out
			case VIRTUAL -> block.add(Bytecode.run(returnType(), allArgs, ctx -> {
					ctx.asm().visitMethodInsn(INVOKEVIRTUAL, owner.internalName(), name(),
							TypeUtils.instanceMethodDescriptor(returnType(), argTypes()), owner.isInterface());
				})).value();
			case INTERFACE -> block.add(Bytecode.run(returnType(), allArgs, ctx -> {
					ctx.asm().visitMethodInsn(INVOKEINTERFACE, owner.internalName(), name(),
							TypeUtils.instanceMethodDescriptor(returnType(), argTypes()), owner.isInterface());
				})).value();
			case SPECIAL -> block.add(Bytecode.run(returnType(), allArgs, ctx -> {
					ctx.asm().visitMethodInsn(INVOKESPECIAL, owner.internalName(), name(),
							TypeUtils.instanceMethodDescriptor(returnType(), argTypes()), owner.isInterface());
				})).value();
			case INIT -> {
				// TODO optimize, this uses unnecessary variables
				var ownerName = owner().internalName();
				
				// Create new object and leave two references of it to stack
				// Don't create value yet - it is not initialized
				block.add(Bytecode.run(Type.VOID, new Value[0], ctx -> {
					ctx.asm().visitTypeInsn(NEW, ownerName);
					ctx.asm().visitInsn(DUP);
				}));
				
				// Load arguments to stack on top of them
				var inputs = new Value[args.length + 1];
				inputs[0] = Value.stackTop(returnType()); // Already on stack
				System.arraycopy(args, 0, inputs, 1, args.length);
				
				// Call constructor to consume one reference to object and all arguments
				var instance = block.add(Bytecode.run(returnType(), inputs, ctx -> {
					ctx.asm().visitMethodInsn(INVOKESPECIAL, ownerName, "<init>",
							TypeUtils.methodDescriptor(Type.VOID, argTypes()), false);
				})).value();
				
				// Return the other reference to the object, now initialized
				yield instance;
			}
			case DYNAMIC -> throw new AssertionError();
			};
		};
	}

	public Handle toMethodHandle() {
		return switch (linkage()) {
		case STATIC -> new Handle(H_INVOKESTATIC, owner().internalName(), name(),
				TypeUtils.methodDescriptor(returnType(), argTypes()), owner().isInterface());
		case VIRTUAL -> new Handle(H_INVOKEVIRTUAL, owner().internalName(), name(),
				TypeUtils.methodDescriptor(returnType(), argTypes()), owner.isInterface());
		case INTERFACE -> new Handle(H_INVOKEINTERFACE, owner().internalName(), name(),
				TypeUtils.methodDescriptor(returnType(), argTypes()), owner.isInterface());
		case SPECIAL -> new Handle(H_INVOKESPECIAL, owner().internalName(), name(),
				TypeUtils.methodDescriptor(Type.VOID, argTypes()), false);
		case INIT -> throw new UnsupportedOperationException(); // Built on top of invokespecial, JVM doesn't REALLY support this
		case DYNAMIC -> throw new AssertionError(); // Should be DynamicCallTarget
		};
	}
	
}
