package fi.benjami.code4jvm.internal.node;

import fi.benjami.code4jvm.internal.DebugNames.Counting;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import fi.benjami.code4jvm.internal.LocalVar;

/**
 * Marker node that is inserted before and after local variable definition.
 * This provides labels needed for the local variable type table.
 *
 */
public final class VarMarkerNode implements Node {
	
	public final boolean start;
	public LocalVar localVar;
	
	public VarMarkerNode(boolean start, LocalVar localVar) {
		this.start = start;
		this.localVar = localVar;
	}

	public void visitLabels(MethodVisitor mv) {
		var label = new Label();
		mv.visitLabel(label);
		if (start) {
			localVar.definitionStart = label;
		} else {
			localVar.definitionEnd = label;
		}
	}
	
	@Override
	public String toString(Counting debugNameGen) {
		return (start ? "BEGIN" : "END") + " DEFINE " + localVar.toString(debugNameGen);
	}
}
