package fi.benjami.code4jvm.statement;

import org.objectweb.asm.Opcodes;

import fi.benjami.code4jvm.Expression;
import fi.benjami.code4jvm.Statement;
import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.Value;

/**
 * Access operations to array contents.
 * 
 * <p>To create arrays, create an {@link Type#array(int) array type} and then
 * a {@link Type#newInstance(Value...) new instance} of it. Provide the array
 * length (as {@code int} value) for each dimension as parameters.
 *
 */
public class ArrayAccess {

	/**
	 * Creates an expression that gets a value from an array.
	 * @param array The array to read from.
	 * @param index Index in the array.
	 * @return Expression that reads a value from the array.
	 */
	public static Expression get(Value array, Value index) {
		return block -> {
			return block.add(Bytecode.run(array.type().componentType(1), new Value[] {array, index}, ctx -> {
				ctx.asm().visitInsn(array.type().getOpcode(Opcodes.IALOAD, ctx));
			})).value();
		};
	}
	
	/**
	 * Creates an expression that sets a value to an array.
	 * @param array The array to mutate.
	 * @param index Index in the array.
	 * @param newValue New value to write.
	 * @return Statement that writes a value to the array.
	 */
	public static Statement set(Value array, Value index, Value newValue) {
		return block -> {
			block.add(Bytecode.run(Type.VOID, new Value[] {array, index, newValue}, ctx -> {
				ctx.asm().visitInsn(array.type().getOpcode(Opcodes.IASTORE, ctx));
			}));
		};
	}
	
	/**
	 * Creates an expression that returns length of an array.
	 * 
	 * <p>Length can also be {@link Value#getField(Type, String) accessed} as
	 * a field named {@code length}, to match the behavior of Java.
	 * @param array The array.
	 * @return Expression that gets array length
	 */
	public static Expression length(Value array) {
		return block -> {
			return block.add(Bytecode.run(Type.INT, new Value[] {array}, ctx -> {
				ctx.asm().visitInsn(Opcodes.ARRAYLENGTH);
			})).value();
		};
	}
}
