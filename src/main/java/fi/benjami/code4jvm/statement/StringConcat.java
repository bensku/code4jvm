package fi.benjami.code4jvm.statement;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.StringConcatFactory;
import java.util.ArrayList;

import org.objectweb.asm.Handle;

import fi.benjami.code4jvm.Constant;
import fi.benjami.code4jvm.Expression;
import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.Value;
import fi.benjami.code4jvm.block.CompileContext;
import fi.benjami.code4jvm.call.CallTarget;
import fi.benjami.code4jvm.config.CoreOptions;
import fi.benjami.code4jvm.util.TypeUtils;

import static org.objectweb.asm.Opcodes.*;

/**
 * String concatenation support.
 * 
 * @implNote Uses {@link StringConcatFactory} with {@code invokedynamic} or
 * {@link StringBuilder} if {@link CoreOptions#INDY_STRING_CONCAT} is disabled.
 */
public class StringConcat {
	
	private static final Handle INDY_CONCAT_BOOTSTRAP = CallTarget.staticMethod(Type.of(StringConcatFactory.class),
					Type.of(CallSite.class), "makeConcatWithConstants",
					Type.of(MethodHandles.Lookup.class), Type.STRING, Type.of(MethodType.class),
					Type.STRING, Type.of(Object.class).array(1)
			).toMethodHandle();
	private static final Type STRING_BUILDER = Type.of(StringBuilder.class);
	
	/**
	 * Concatenates the given values together to form a string. This works
	 * similarly to string concatenation in Java.
	 * @param values Values to convert to strings and concatenate.
	 * @return Expression that creates the concatenated string value.
	 */
	public static Expression concat(Value... values) {
		return block -> {
			return block.add(Bytecode.run(Type.STRING, values, ctx -> {
				// StringConcatFactory supports at most 199 values
				// For bigger operations, fall back to StringBuilder for now
				if (ctx.options().get(CoreOptions.INDY_STRING_CONCAT) && values.length < 200) {
					indyConcat(ctx, values);
				} else {
					stringBuilderConcat(ctx, values);
				}
			}, Bytecode.EXPLICIT_LOAD, "string concat")).value();
		};
	}
	
	private static void indyConcat(CompileContext ctx, Value[] values) {
		var mv = ctx.asm();
		
		var recipe = new StringBuilder();
		var inputs = new ArrayList<Value>();
		var constants = new ArrayList<Constant>();
		
		// Separate runtime inputs and (different styles of) constants
		for (var value : values) {
			if (value instanceof Constant constant) {
				var constStr = constant.value().toString();
				// TODO make sure dynamic constants are not added to recipe (at least not by default)
				if (!constStr.contains("\1") && !constStr.contains("\2")) {
					// Constants that don't contain \1 or \2 can be added to recipe as-is
					recipe.append(constStr);
				} else {
					recipe.append('\2'); // Constant
					constants.add(constant);
				}
			} else {
				recipe.append('\1'); // Ordinary input
				inputs.add(value);
			}
		}
		// Load non-constant values to stack for invokedynamic call
		ctx.loadExplicit(inputs.toArray(Value[]::new));
		
		// Build the invokedynamic call
		var descriptor = TypeUtils.methodDescriptor(Type.STRING, inputs.stream()
				.map(Value::type)
				.toArray(Type[]::new)
		);
		var bootstrapArgs = new Object[constants.size() + 1];
		bootstrapArgs[0] = recipe.toString();
		for (int i = 0; i < constants.size(); i++) {
			bootstrapArgs[i + 1] = constants.get(i).asmValue();
		}
		mv.visitInvokeDynamicInsn("_", descriptor, INDY_CONCAT_BOOTSTRAP, bootstrapArgs);
	}
	
	private static void stringBuilderConcat(CompileContext ctx, Value[] values) {
		var mv = ctx.asm();
		
		// Create StringBuilder
		mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
		mv.visitInsn(DUP);
		mv.visitMethodInsn(INVOKESPECIAL, STRING_BUILDER.internalName(),
				"<init>", TypeUtils.methodDescriptor(Type.VOID), false);
		
		for (var value : values) {
			var type = value.type();
			ctx.loadExplicit(value);
			if (type.equals(Type.BOOLEAN)) {
				mv.visitMethodInsn(INVOKEVIRTUAL, STRING_BUILDER.internalName(),
						"append", TypeUtils.methodDescriptor(STRING_BUILDER, Type.BOOLEAN), false);
			} else if (type.equals(Type.CHAR)) {
				mv.visitMethodInsn(INVOKEVIRTUAL, STRING_BUILDER.internalName(),
						"append", TypeUtils.methodDescriptor(STRING_BUILDER, Type.CHAR), false);
			} else if (TypeUtils.isIntLike(type)) {
				// StringBuilder uses int parameter for most things JVM considers ints
				// (except char, because it was originally designed to represent a character)
				mv.visitMethodInsn(INVOKEVIRTUAL, STRING_BUILDER.internalName(),
						"append", TypeUtils.methodDescriptor(STRING_BUILDER, Type.INT), false);
			} else if (type.equals(Type.LONG)) {
				mv.visitMethodInsn(INVOKEVIRTUAL, STRING_BUILDER.internalName(),
						"append", TypeUtils.methodDescriptor(STRING_BUILDER, Type.LONG), false);
			} else if (type.equals(Type.FLOAT)) {
				mv.visitMethodInsn(INVOKEVIRTUAL, STRING_BUILDER.internalName(),
						"append", TypeUtils.methodDescriptor(STRING_BUILDER, Type.FLOAT), false);
			} else if (type.equals(Type.DOUBLE)) {
				mv.visitMethodInsn(INVOKEVIRTUAL, STRING_BUILDER.internalName(),
						"append", TypeUtils.methodDescriptor(STRING_BUILDER, Type.DOUBLE), false);
			} else {
				// Strings are objects, so special case for them should not be necessary
				mv.visitMethodInsn(INVOKEVIRTUAL, STRING_BUILDER.internalName(),
						"append", TypeUtils.methodDescriptor(STRING_BUILDER, Type.OBJECT), false);
			}
			// append(...) returns (places to stack) the StringBuilder
		}
		
		// Leave the final string on stack
		mv.visitMethodInsn(INVOKEVIRTUAL, STRING_BUILDER.internalName(),
				"toString", TypeUtils.methodDescriptor(Type.STRING), false);
	}
}
