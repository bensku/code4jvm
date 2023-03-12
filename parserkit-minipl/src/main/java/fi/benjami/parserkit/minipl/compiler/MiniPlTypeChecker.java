package fi.benjami.parserkit.minipl.compiler;

import fi.benjami.parserkit.minipl.parser.MiniPlVisitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import fi.benjami.code4jvm.Type;
import fi.benjami.parserkit.minipl.parser.MiniPlNodes.*;

public class MiniPlTypeChecker implements MiniPlVisitor {
	
	public static final Type ERROR_TYPE = Type.of("minipl.error", false);

	private final Map<String, Type> varTypes;
	private final Map<Expression, Type> exprTypes;
	private final List<SemanticError> errors;
	
	public MiniPlTypeChecker() {
		this.varTypes = new HashMap<>();
		this.exprTypes = new IdentityHashMap<>();
		this.errors = new ArrayList<>();
	}
	
	@Override
	public void visit(VarDeclaration node) {
		var type = parseType(node.type());
		if (varTypes.containsKey(node.name())) {
			errors.add(SemanticError.of(SemanticError.Type.VARIABLE_ALREADY_DEFINED, node.name()));
			// Allow overwriting the type, this won't compile anyway
		}
		if (type.equals(MiniPlTypeChecker.ERROR_TYPE)) {
			errors.add(SemanticError.of(SemanticError.Type.UNKNOWN_TYPE, node.name(), node.type()));
		}
		varTypes.put(node.name(), type);
		
		if (node.initialValue() != null) {
			expectType(type, exprType(node.initialValue()));
		}
	}
	
	@Override
	public void visit(VarAssignment node) {
		var type = varTypes.get(node.name());
		if (type == null) {
			errors.add(SemanticError.of(SemanticError.Type.VARIABLE_NOT_DEFINED, node.name()));
			// Define type for error recovery
			varTypes.put(node.name(), exprType(node.value()));
		} else {
			// Check that type of value assigned is correct
			expectType(type, exprType(node.value()));
		}
	}
	
	@Override
	public void visit(BuiltinRead node) {
		var type = varTypes.get(node.variable());
		if (type == null) {
			errors.add(SemanticError.of(SemanticError.Type.VARIABLE_ALREADY_DEFINED, node.variable()));
		} else {
			if (!type.equals(Type.STRING) && !type.equals(Type.INT) && !type.equals(ERROR_TYPE)) {
				errors.add(SemanticError.of(SemanticError.Type.TYPE_CONFLICT, "string or int", type));
			}
		}
	}
	
	@Override
	public void visit(BuiltinPrint node) {
		var type = exprType(node.expr());
		if (!type.equals(Type.STRING) && !type.equals(Type.INT) && !type.equals(ERROR_TYPE)) {
			errors.add(SemanticError.of(SemanticError.Type.TYPE_CONFLICT, "string or int", type));
		}
	}
	
	@Override
	public void visit(ForBlock node) {
		expectType(Type.INT, varType(node.counter()));
		expectType(Type.INT, node.start(), node.end());
	}
	
	@Override
	public void visit(Group node) {
		saveExprType(node, node.expr());
	}
	
	@Override
	public void visit(LogicalNotExpr node) {
		expectType(Type.BOOLEAN, node.expr());
		exprTypes.put(node, Type.BOOLEAN);
	}
	
	@Override
	public void visit(LogicalAndExpr node) {
		expectType(Type.BOOLEAN, node.lhs(), node.rhs());
		exprTypes.put(node, Type.BOOLEAN);
	}
	@Override
	public void visit(EqualsExpr node) {
		saveExprType(node, node.lhs(), node.rhs());
	}
	
	@Override
	public void visit(LessThanExpr node) {
		expectType(Type.INT, node.lhs(), node.rhs());
		exprTypes.put(node, Type.INT);
	}
	
	@Override
	public void visit(AddExpr node) {
		var type = exprType(node.lhs());
		if (!type.equals(Type.STRING) && !type.equals(Type.INT)) {
			errors.add(SemanticError.of(SemanticError.Type.TYPE_CONFLICT, "string or int", type));
		}
		saveExprType(node, node.lhs(), node.rhs());
	}
	
	@Override
	public void visit(SubtractExpr node) {
		expectType(Type.INT, node.lhs(), node.rhs());
		exprTypes.put(node, Type.INT);
	}
	
	@Override
	public void visit(MultiplyExpr node) {
		expectType(Type.INT, node.lhs(), node.rhs());
		exprTypes.put(node, Type.INT);
	}
	
	@Override
	public void visit(DivideExpr node) {
		expectType(Type.INT, node.lhs(), node.rhs());
		exprTypes.put(node, Type.INT);
	}
	
	@Override
	public void visit(Literal node) {
		var value = node.value();
		if (value instanceof Constant constant) {
			var boxedType = constant.value().getClass();
			Type type;
			// Unbox boxed int and boolean
			if (boxedType.equals(Integer.class)) {
				type = Type.INT;
			} else if (boxedType.equals(Boolean.class)) {
				type = Type.BOOLEAN;
			} else {
				type = Type.of(boxedType);
			}
			exprTypes.put(node, type);
		} else if (value instanceof VarReference varRef) {
			exprTypes.put(node, varType(varRef.variable()));
		}
	}
	
	private Type parseType(String type) {
		return switch (type) {
		case "int" -> Type.INT;
		case "string" -> Type.STRING;
		case "bool" -> Type.BOOLEAN;
		default -> MiniPlTypeChecker.ERROR_TYPE;
		};
	}
	
	private void saveExprType(Expression node, Expression... children) {
		// Check that all children have same types
		var type = exprType(children[0]);
		for (int i = 1; i < children.length; i++) {
			expectType(type, exprType(children[i]));
		}
		
		// Store the first type as type of this expression
		exprTypes.put(node, type);
	}
	
	private void expectType(Type expected, Type actual) {
		// Suppress errors on ERROR_TYPE, we already have an error about that
		if (!expected.equals(actual) && !actual.equals(ERROR_TYPE)) {
			errors.add(SemanticError.of(SemanticError.Type.TYPE_CONFLICT, expected.simpleName(), actual.simpleName()));
		}
	}
	
	private void expectType(Type expected, Expression... exprs) {
		for (var expr : exprs) {
			expectType(expected, exprType(expr));
		}
	}
	
	public Type varType(String varName) {
		var type = varTypes.get(varName);
		if (type == null) {
			errors.add(SemanticError.of(SemanticError.Type.VARIABLE_NOT_DEFINED, varName));
			type = MiniPlTypeChecker.ERROR_TYPE;
		}
		return type;
	}
	
	public Type exprType(Expression node) {
		return exprTypes.get(node);
	}
	
	public List<SemanticError> errors() {
		return errors;
	}
	
	public TypeTable toTypeTable() {
		if (!errors.isEmpty()) {
			throw new IllegalStateException("errors found, cannot create type table");
		}
		return new TypeTable(varTypes, exprTypes);
	}
	
	
}
