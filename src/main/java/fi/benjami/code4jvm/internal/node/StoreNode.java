package fi.benjami.code4jvm.internal.node;

import static org.objectweb.asm.Opcodes.ISTORE;
import static org.objectweb.asm.Opcodes.POP;
import static org.objectweb.asm.Opcodes.POP2;

import fi.benjami.code4jvm.Statement;
import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.Value;
import fi.benjami.code4jvm.Variable;
import fi.benjami.code4jvm.block.Block;
import fi.benjami.code4jvm.block.StackManager;
import fi.benjami.code4jvm.internal.LocalVar;
import fi.benjami.code4jvm.internal.MethodCompilerState;
import fi.benjami.code4jvm.internal.DebugNames;

/**
 * Node that represents storing a value to a variable.
 *
 * <p>Storing to {@link Variable#create(fi.benjami.code4jvm.Type)
 * unbound variables} makes them available. For this reason, we need to track
 * stores while building stack map frames, which requires a custom node type.
 */
public record StoreNode(
		LocalVar target,
		Value value
) implements Node, Statement {
	
	@Override
	public void emitVoid(Block block) {
		throw new UnsupportedOperationException();
	}
	
	public void emitBytecode(MethodCompilerState state) {
		if (target.needsSlot) {
			// Target is used and cannot just consume the value from stack
			assert target.assignedSlot != -1 : "tried to store to no slot";
			loadValue(state.ctx().stack());
			state.ctx().asm().visitVarInsn(target.type().getOpcode(ISTORE, state.ctx()),
					target.assignedSlot);
		} else if (!target.used && (!(value instanceof LocalVar localVar) || !localVar.needsSlot)) {
			// Target is NOT used and value is likely on stack -> remove it
			loadValue(state.ctx().stack());
			var type = value.type();
			if (type == Type.LONG || type == Type.DOUBLE) {
				state.ctx().asm().visitInsn(POP2);
			} else {			
				state.ctx().asm().visitInsn(POP);
			}
		}
	}
	
	private void loadValue(StackManager stack) {
		stack.reserveStack(value); // Tell stack manager that we need to
		stack.loadExplicit(value); // ... load the value to stack
	}
	
	@Override
	public String toString(DebugNames.Counting debugNameGen) {
		return target.toString(debugNameGen) + " = " + value;
	}

}
