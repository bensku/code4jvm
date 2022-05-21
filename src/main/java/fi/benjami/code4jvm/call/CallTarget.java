package fi.benjami.code4jvm.call;

import java.util.Arrays;

import fi.benjami.code4jvm.ClassDef;
import fi.benjami.code4jvm.Expression;
import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.Value;
import fi.benjami.code4jvm.flag.Access;
import fi.benjami.code4jvm.flag.MethodFlag;
import fi.benjami.code4jvm.statement.Return;

/**
 * A target for method calls, i.e. a method or dynamic target.
 *
 */
public abstract sealed class CallTarget permits FixedCallTarget, DynamicCallTarget {
	
	public static FixedCallTarget staticMethod(Type owner, Type returnType, String name, Type... argTypes) {
		return new FixedCallTarget(returnType, name, argTypes, Linkage.STATIC, new Value[0], owner);
	}
	
	public static FixedCallTarget virtualMethod(Type owner, Type returnType, String name, Type... argTypes) {
		return new FixedCallTarget(returnType, name, withImplicitThis(owner, argTypes), owner.isInterface()
				? Linkage.INTERFACE : Linkage.VIRTUAL, new Value[0], owner);
	}
	
	public static FixedCallTarget privateMethod(Type owner, Type returnType, String name, Type... argTypes) {
		return new FixedCallTarget(returnType, name, withImplicitThis(owner, argTypes),
				Linkage.SPECIAL, new Value[0], owner);
	}
	
	public static FixedCallTarget constructor(Type owner, Type... argTypes) {
		// Custom linkage INIT handles heavy lifting
		return new FixedCallTarget(owner, "<init>", argTypes, Linkage.INIT, new Value[0], owner);
	}
	
	public static DynamicCallTarget dynamic(FixedCallTarget bootstrapMethod, Type returnType, String name, Type... argTypes) {
		return new DynamicCallTarget(returnType, name, argTypes, new Value[0], bootstrapMethod);
	}
	
	static Type[] withImplicitThis(Type owner, Type[] argTypes) {
		var allArgs = new Type[argTypes.length + 1];
		allArgs[0] = owner;
		System.arraycopy(argTypes, 0, allArgs, 1, argTypes.length);
		return allArgs;
	}
	
	static Value[] mergeArgs(Value[] capturedArgs, Value[] args) {
		var allCaptures = Arrays.copyOf(capturedArgs, capturedArgs.length + args.length);
		System.arraycopy(args, 0, allCaptures, capturedArgs.length, args.length);
		return allCaptures;
	}
	
	private final Type returnType;
	private final String name;
	private final Type[] argTypes;
	private final Linkage linkage;
	private final Value[] capturedArgs;
		
	CallTarget(Type returnType, String name, Type[] argTypes, Linkage linkage, Value[] capturedArgs) {
		this.returnType = returnType;
		this.name = name;
		this.argTypes = argTypes;
		this.linkage = linkage;
		this.capturedArgs = capturedArgs;
	}
	
	public Type returnType() {
		return returnType;
	}
	
	public String name() {
		return name;
	}
	
	public Type[] argTypes() {
		return argTypes;
	}
	
	public Linkage linkage() {
		return linkage;
	}
	
	public Value[] capturedArgs() {
		return capturedArgs;
	}
	
	/**
	 * Creates a copy of this target with given captured arguments. If this
	 * target already captures arguments, they are included before new captured
	 * arguments.
	 * @param args Arguments to capture.
	 * @return A new call target with more captured arguments.
	 */
	public abstract CallTarget withCapturedArgs(Value... args);
	
	/**
	 * Creates a copy of this target without any of the captured arguments.
	 * @return A new call target without captured arguments.
	 */
	public abstract CallTarget withoutCapturedArgs();
	
	/**
	 * Creates a call to this target.
	 * @param args Argument values for the call.
	 * @return Method call expression.
	 */
	public abstract Expression call(Value... args);
	
	/**
	 * Creates a new static bridge method that calls this target.
	 * @param def Class where to add the bridge.
	 * @param access Visibility of the bridge method.
	 * @return A call target to the bridge method.
	 */
	public FixedCallTarget newBridge(ClassDef def, Access access) {
		var bridgeName = "bridge$" + def.methods().size();
		var method = def.addStaticMethod(returnType, bridgeName, access, MethodFlag.BRIDGE);
		
		// Prepare arguments to be equivalent to target's arguments
		var bridgeArgs = new Value[argTypes.length];
		for (int i = 0; i < argTypes.length; i++) {
			bridgeArgs[i] = method.arg(argTypes[i]);
		}
		
		// Call the target and return what it returned
		var result = method.add(call(bridgeArgs)).value();
		method.add(Return.value(result));
		
		return CallTarget.staticMethod(def.type(), returnType, bridgeName, argTypes);
	}
	
}
