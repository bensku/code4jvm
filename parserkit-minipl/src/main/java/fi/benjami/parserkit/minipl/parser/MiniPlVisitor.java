package fi.benjami.parserkit.minipl.parser;

import static fi.benjami.parserkit.minipl.parser.MiniPlNodes.*;

public interface MiniPlVisitor {

	default void visit(Program node) {}
	default void visit(Block node) {}
	
	default void visit(VarDeclaration node) {}
	default void visit(VarAssignment node) {}
	default void visit(BuiltinRead node) {}
	default void visit(BuiltinPrint node) {}
	default void visit(IfBlock node) {}
	default void visit(ForBlock node) {}

	default void visit(Group node) {}
	default void visit(LogicalNotExpr node) {}
	default void visit(LogicalAndExpr node) {}
	default void visit(EqualsExpr node) {}
	default void visit(LessThanExpr node) {}
	default void visit(AddExpr node) {}
	default void visit(SubtractExpr node) {}
	default void visit(MultiplyExpr node) {}
	default void visit(DivideExpr node) {}
	default void visit(Literal node) {}
	
	default void visit(Constant node) {}
	default void visit(VarReference node) {}

}
