package fi.benjami.code4jvm.internal;

import fi.benjami.code4jvm.statement.Bytecode;

public class CodeNode implements Node {
	
	private final Bytecode statement;
	private LocalVar assignedVar;
	
	public CodeNode(Bytecode statement) {
		this.statement = statement;
	}
	
	public void assignVar(LocalVar localVar) {
		this.assignedVar = localVar;
	}
	
	public void emitBytecode(CompileContext ctx) {
		statement.emitBytecode(ctx);
		if (assignedVar != null) {
			statement.storeOutput(ctx, assignedVar);
		} else {
			statement.discardOutput(ctx);
		}
	}
}
