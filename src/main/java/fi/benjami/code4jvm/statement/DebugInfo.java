package fi.benjami.code4jvm.statement;

import org.objectweb.asm.Label;

import fi.benjami.code4jvm.Statement;
import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.Value;

/**
 * Tools for emitting debug information in methods.
 *
 */
public class DebugInfo {
	
	/**
	 * Creates a statement that marks beginning of a line where it is inserted.
	 * @param number Line number.
	 * @return Statement that adds debug information.
	 */
	public static Statement lineNumber(int number) {
		return block -> {
			block.add(Bytecode.run(Type.VOID, new Value[0], ctx -> {
				var label = new Label();
				ctx.asm().visitLabel(label);
				ctx.asm().visitLineNumber(number, label);
			}, Bytecode.name("line %s", number)));
		};
	}
}
