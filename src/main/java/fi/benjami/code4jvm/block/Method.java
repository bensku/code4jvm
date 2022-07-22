package fi.benjami.code4jvm.block;

import java.util.List;

import org.objectweb.asm.Opcodes;

import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.Value;
import fi.benjami.code4jvm.flag.MethodFlag;
import fi.benjami.code4jvm.internal.LocalVar;

/**
 * A method that can be added to a class.
 * 
 * @see Lambda
 *
 */
public sealed interface Method permits AbstractMethod, ConcreteMethod {
	
	public static Method.Static staticMethod(Type returnType, String name, MethodFlag... flags) {
		return new Method.Static(Block.create(), returnType, name, flags);
	}
	
	public static Method.Instance instanceMethod(Type returnType, String name, Type parentClass, MethodFlag... flags) {
		return new Method.Instance(Block.create(), returnType, name, parentClass, flags);
	}
	
	public static AbstractMethod abstractMethod(Type returnType, String name, MethodFlag... flags) {
		return new AbstractMethod(returnType, name, flags, false, false);
	}
	
	public static AbstractMethod nativeMethod(boolean isStatic, Type returnType, String name, MethodFlag... flags) {
		return new AbstractMethod(returnType, name, flags, isStatic, true);
	}
	
	static int getAccess(MethodFlag[] flags) {
		var access = 0;
		// JVM stores other flags in access bit set, OR them into it
		for (var flag : flags) {
			access |= flag.value();
		}
		return access;
	}
	
	public static final class Static extends ConcreteMethod {
		Static(Block block, Type returnType, String name, MethodFlag[] flags) {
			super(block, returnType, name, getAccess(flags) | Opcodes.ACC_STATIC);
		}
	}
	
	public static final class Instance extends ConcreteMethod {
		
		/**
		 * Value for 'this'.
		 */
		final LocalVar self;
		
		Instance(Block block, Type returnType, String name, Type parentClass, MethodFlag[] flags) {
			super(block, returnType, name, getAccess(flags));
			// this is passed in slot 0
			// It is NOT present in method signature, so avoid calling arg()
			this.self = new LocalVar(parentClass, true);
			argsAllocator.get(self);
		}
		
		public Value self() {
			return self;
		}
	}
	
	Type returnType();
	
	String name();
	
	List<Type> argumentTypes();

}
