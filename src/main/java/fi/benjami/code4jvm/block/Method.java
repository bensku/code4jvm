package fi.benjami.code4jvm.block;

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
public class Method extends Routine {
	
	public static Method.Static staticMethod(Type returnType, String name, MethodFlag... flags) {
		return new Method.Static(Block.create(), returnType, name, flags);
	}
	
	public static Method.Instance instanceMethod(Type returnType, String name, Type parentClass, MethodFlag... flags) {
		return new Method.Instance(Block.create(), returnType, name, parentClass, flags);
	}
	
	private static int getAccess(MethodFlag[] flags) {
		var access = 0;
		// JVM stores other flags in access bit set, OR them into it
		for (var flag : flags) {
			access |= flag.value();
		}
		return access;
	}
	
	public static class Static extends Method {
		Static(Block block, Type returnType, String name, MethodFlag[] flags) {
			super(block, returnType, name, getAccess(flags) | Opcodes.ACC_STATIC);
		}
	}
	
	public static class Instance extends Method {
		
		/**
		 * Value for 'this'.
		 */
		private final LocalVar self;
		
		Instance(Block block, Type returnType, String name, Type parentClass, MethodFlag[] flags) {
			super(block, returnType, name, getAccess(flags));
			// this is passed in slot 0
			// It is NOT present in method signature, so avoid calling arg()
			this.self = new LocalVar(parentClass, block);
			self.initialized = true; // this is always initialized
			argsAllocator.get(self);
		}
		
		public Value self() {
			return self;
		}
	}

	private final String name;
	final int access;
	
	Method(Block block, Type returnType, String name, int access) {
		super(block, returnType);
		this.name = name;
		this.access = access;
	}
	
	public String name() {
		return name;
	}

}
