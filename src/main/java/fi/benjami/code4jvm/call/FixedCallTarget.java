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
		
		// Validate that the correct number of arguments were provided
		// We can't validate types of the arguments, because the runtime type hierarchy is not known
		var argTypes = argTypes();
		if (allArgs.length != argTypes.length) {
			throw new IllegalArgumentException("expected " + argTypes().length + " arguments, got " + allArgs.length);
		}
		
		var debugName = Bytecode.name("call %s", this);
		return block -> {
			return switch (linkage()) {
			case STATIC -> block.add(Bytecode.run(returnType(), allArgs, ctx -> {
					ctx.asm().visitMethodInsn(INVOKESTATIC, owner.internalName(), name(),
							TypeUtils.methodDescriptor(returnType(), argTypes()), owner.isInterface());
				}, debugName));
			case VIRTUAL -> block.add(Bytecode.run(returnType(), allArgs, ctx -> {
					ctx.asm().visitMethodInsn(INVOKEVIRTUAL, owner.internalName(), name(),
							TypeUtils.instanceMethodDescriptor(returnType(), argTypes()), owner.isInterface());
				}, debugName));
			case INTERFACE -> block.add(Bytecode.run(returnType(), allArgs, ctx -> {
					ctx.asm().visitMethodInsn(INVOKEINTERFACE, owner.internalName(), name(),
							TypeUtils.instanceMethodDescriptor(returnType(), argTypes()), owner.isInterface());
				}, debugName));
			case SPECIAL -> block.add(Bytecode.run(returnType(), allArgs, ctx -> {
					ctx.asm().visitMethodInsn(INVOKESPECIAL, owner.internalName(), name(),
							TypeUtils.instanceMethodDescriptor(returnType(), argTypes()), owner.isInterface());
				}, debugName));
			case INIT -> block.add(Bytecode.run(returnType(), allArgs, ctx -> {
					var ownerName = owner().internalName();
					
					// Reserve stack space for two copies of initialized type
					ctx.stack().reserveStack(TypeUtils.slotCount(returnType()) * 2);
					ctx.asm().visitTypeInsn(NEW, ownerName);
					ctx.asm().visitInsn(DUP);
					
					ctx.stack().loadExplicit(allArgs);
					ctx.asm().visitMethodInsn(INVOKESPECIAL, ownerName, "<init>",
							TypeUtils.methodDescriptor(Type.VOID, argTypes()), false);
					// We consumed one of the instances and left another on the stack
				}, Bytecode.EXPLICIT_LOAD, debugName));
			case INIT_ARRAY -> {
				var type = returnType();
				if (type.arrayDimensions() > 1) {
					// Multi-dimensional primitive or reference array
					// This could probably be used for other kinds of arrays, but currently we don't
					yield block.add(Bytecode.run(returnType(), allArgs, ctx -> {
						ctx.asm().visitMultiANewArrayInsn(type.descriptor(), type.arrayDimensions());
					}, debugName));
				} else if (type.isPrimitive()) {
					// One-dimensional primitive array
					yield block.add(Bytecode.run(returnType(), allArgs, ctx -> {
						// JVM treats many Java primitives as ints, EXCEPT it has array types for them
						// Each of them needs its own operand
						ctx.asm().visitIntInsn(NEWARRAY, TypeUtils.getNewarrayOperand(type.componentType(1)));
					}, debugName));
				} else {
					// One-dimensional object array
					yield block.add(Bytecode.run(returnType(), allArgs, ctx -> {
						ctx.asm().visitTypeInsn(ANEWARRAY, type.internalName());
					}, debugName));
				}
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
		// INIT and INIT_ARRAY are code4jvm's own helpers, not supported by the JVM
		case INIT -> throw new UnsupportedOperationException();
		case INIT_ARRAY -> throw new UnsupportedOperationException();
		case DYNAMIC -> throw new AssertionError(); // Should be DynamicCallTarget
		};
	}
	
	@Override
	public String toString() {
		return linkage() + " " + owner + "#" + name();
	}

}
