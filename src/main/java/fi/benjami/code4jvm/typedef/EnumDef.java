package fi.benjami.code4jvm.typedef;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import fi.benjami.code4jvm.Constant;
import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.Value;
import fi.benjami.code4jvm.block.Block;
import fi.benjami.code4jvm.block.Method;
import fi.benjami.code4jvm.flag.Access;
import fi.benjami.code4jvm.flag.ClassFlag;
import fi.benjami.code4jvm.flag.FieldFlag;
import fi.benjami.code4jvm.flag.MethodFlag;
import fi.benjami.code4jvm.statement.ArrayAccess;
import fi.benjami.code4jvm.statement.Return;

/**
 * A wrapper for {@link ClassDef class builder} for creating Java-like enums.
 * 
 * @implNote Only public, high-level code4jvm APIs are used. There is no
 * bytecode generation here.
 *
 */
public class EnumDef extends ClassDef {
	
	/**
	 * Creates a new enum builder.
	 * @param name Fully qualified class name.
	 * @param access Allowed access.
	 * @param flags Class flags. Note that enums cannot be
	 * {@link ClassFlag#ABSTRACT abstract}!
	 * @return A new enum builder.
	 */
	public static EnumDef create(String name, Access access, ClassFlag... flags) {
		for (var flag : flags) {
			if (flag == ClassFlag.ABSTRACT) {
				throw new IllegalArgumentException("enums cannot be abstract");
			}
		}
		// Append ENUM flag in case it was missing
		var allFlags = Arrays.copyOf(flags, flags.length + 1);
		allFlags[flags.length] = ClassFlag.ENUM;
		return new EnumDef(name, access, allFlags);
	}
		
	private record EnumConstant(String name, Block initializer, Value[] args) {}
		
	private final List<EnumConstant> constants;
	
	private boolean compiled;
	
	private EnumDef(String name, Access access, ClassFlag... flags) {
		super(name, access, flags);
		this.constants = new ArrayList<>();
		superClass(Type.of(Enum.class));
		setCompileHook(this, this::finishEnum);
	}
	
	private void finishEnum(ClassDef def) {
		if (compiled) {
			return;
		}
		compiled = true;
		
		// Create (internal) values array
		var valuesType = type().array(1);
		addStaticField(Access.PRIVATE, valuesType, "$VALUES", FieldFlag.SYNTHETIC);
		
		// Initialize enum constants and values array
		addClassInit();
		
		// Create static enum methods
		addValuesMethod();
		addValueOfMethod();
	}
	
	private void addClassInit() {
		var valuesType = type().array(1);
		var classInit = addStaticMethod(Type.VOID, "<clinit>", Access.PRIVATE, MethodFlag.SYNTHETIC);
		// Store values array we'll put fields to as $VALUES
		var values = classInit.add(valuesType.newInstance(Constant.of(constants.size())));
		classInit.add(type().putStatic(valuesType, "$VALUES", values));
		for (int i = 0; i < constants.size(); i++) {
			var constant = constants.get(i);
			if (constant.initializer != null) {
				// Initializer could be missing if all arguments are constants
				classInit.add(constant.initializer);
			}
			
			// Constructor takes 2 "hidden" arguments, plus the user-specified ones
			var allArgs = new Value[constant.args.length + 2];
			allArgs[0] = Constant.of(constant.name);
			allArgs[1] = Constant.of(i);
			System.arraycopy(constant.args, 0, allArgs, 2, constant.args.length);
			var enumInstance = classInit.add(type().newInstance(allArgs));
			classInit.add(type().putStatic(type(), constant.name, enumInstance));
			classInit.add(ArrayAccess.set(values, allArgs[1], enumInstance));
		}
		classInit.add(Return.nothing());
	}
	
	private void addValuesMethod() {
		var valuesType = type().array(1);
		var method = addStaticMethod(valuesType, "values", Access.PUBLIC);
		var values = method.add(type().getStatic(valuesType, "$VALUES"));
		method.add(Return.value(values));
	}
	
	private void addValueOfMethod() {
		var method = addStaticMethod(type(), "valueOf", Access.PUBLIC);
		var name = method.arg(Type.STRING);
		// valueOf(String) of all enums is implemented by calling Enum#valueOf(Class, String)
		var value = method.add(Type.of(Enum.class).callStatic(Type.of(Enum.class), "valueOf",
				Constant.of(type()), name));
		method.add(Return.value(value.cast(type())));
	}
	
	@Override
	public void addMethod(Method method, Access access) {
		// Patch all constructors to include arguments for superclass j.l.Enum
		if (method instanceof Method.Instance constructor && method.name().equals("<init>")) {
			if (access != Access.PRIVATE) {
				throw new IllegalArgumentException("enum constructors must be private");
			}
			// Add arguments for enum constant name and ordinal
			var name = constructor.arg(Type.STRING, null, 0);
			var ordinal = constructor.arg(Type.INT, null, 1);
			
			// Use arguments in call to super-constructor
			var block = Block.create();
			block.add(constructor.self().callPrivate(Type.of(Enum.class), Type.VOID, "<init>", name, ordinal));
			
			// Patch before user code in the constructor
			// This might cause the constructor to call multiple constructors
			// (i.e. this(...) after our super(...) call), but JVM allows this
			constructor.block().patchToStart(block);
		}
		super.addMethod(method, access);
	}
	
	@Override
	public void addEmptyConstructor(Access access) {
		var constructor = addConstructor(access);
		// Overridden addMethod will patch super call in
		constructor.add(Return.nothing());
	}
	
	/**
	 * Adds a new constant to this enum that uses only constants as arguments
	 * to the enum constructor.
	 * @param name Constant name.
	 * @param constructorArgs Constant arguments for the enum constructor.
	 */
	public void addEnumConstant(String name, Constant... constructorArgs) {
		Objects.requireNonNull(name);
		Objects.requireNonNull(constructorArgs);
		addStaticField(Access.PUBLIC, type(), name, FieldFlag.ENUM, FieldFlag.FINAL);
		constants.add(new EnumConstant(name, null, constructorArgs));
	}
	
	/**
	 * Adds a new constant to this enum.
	 * @param name Constant name.
	 * @param initializer Block that initializes the arguments for enum 
	 * constructor.
	 * @param constructorArgs The argument values from initializer.
	 */
	public void addEnumConstant(String name, Block initializer, Value... constructorArgs) {
		Objects.requireNonNull(name);
		Objects.requireNonNull(initializer);
		Objects.requireNonNull(constructorArgs);
		addStaticField(Access.PUBLIC, type(), name, FieldFlag.ENUM, FieldFlag.FINAL);
		constants.add(new EnumConstant(name, initializer, constructorArgs));
	}
	
	/**
	 * Adds a new constant to this enum.
	 * @param name Constant name.
	 * @param argProvider Argument provider that receives the block that should
	 * initialize the arguments for enum constructor, and return the values of
	 * those arguments.
	 */
	public void addEnumConstant(String name, Function<Block, Value[]> argProvider) {
		var initializer = Block.create();
		var args = argProvider.apply(initializer);
		addEnumConstant(name, initializer, args);
	}
	
}
