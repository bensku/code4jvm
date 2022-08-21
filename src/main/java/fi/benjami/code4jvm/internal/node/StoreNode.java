package fi.benjami.code4jvm.internal.node;

import static org.objectweb.asm.Opcodes.ISTORE;

import fi.benjami.code4jvm.Statement;
import fi.benjami.code4jvm.Value;
import fi.benjami.code4jvm.Variable;
import fi.benjami.code4jvm.block.Block;
import fi.benjami.code4jvm.internal.LocalVar;
import fi.benjami.code4jvm.internal.MethodCompilerState;

/**
 * Node that represents storing a value to a variable.
 *
 * <p>Storing to {@link Variable#createUnbound(fi.benjami.code4jvm.Type)
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
			assert target.assignedSlot != -1 : "tried to store to no slot";
			state.ctx().loadExplicit(value);
			state.ctx().asm().visitVarInsn(target.type().getOpcode(ISTORE, state.ctx()),
					target.assignedSlot);
		}
	}

}
