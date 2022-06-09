package fi.benjami.code4jvm.internal;

import fi.benjami.code4jvm.statement.Bytecode;

public final class CodeNode implements Node {
	
	private final Bytecode statement;
	private LocalVar assignedVar;
	
	public CodeNode(Bytecode statement) {
		this.statement = statement;
	}
	
	public void assignVar(LocalVar localVar) {
		this.assignedVar = localVar;
	}
	
	public void emitBytecode(MethodCompilerState state) {
		statement.emitBytecode(state);
		if (assignedVar != null) {
			statement.storeOutput(state, assignedVar);
		} else {
			statement.discardOutput(state.ctx());
		}
	}
}
