package fi.benjami.parserkit.minipl.compiler;

import java.util.HashMap;
import java.util.Map;

import fi.benjami.code4jvm.Condition;
import fi.benjami.code4jvm.Constant;
import fi.benjami.code4jvm.Expression;
import fi.benjami.code4jvm.Statement;
import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.Value;
import fi.benjami.code4jvm.Variable;
import fi.benjami.code4jvm.block.Block;
import fi.benjami.code4jvm.flag.Access;
import fi.benjami.code4jvm.statement.Arithmetic;
import fi.benjami.code4jvm.statement.Return;
import fi.benjami.code4jvm.structure.IfBlock;
import fi.benjami.code4jvm.structure.LoopBlock;
import fi.benjami.code4jvm.typedef.ClassDef;
import fi.benjami.parserkit.minipl.MiniPlRuntime;
import fi.benjami.parserkit.minipl.parser.MiniPlNodes;

public class MiniPlCompiler {

	private final TypeTable types;
	private final Map<String, Variable> variables;
	private final MiniPlNodes.Program program;
	
	public MiniPlCompiler(TypeTable types, MiniPlNodes.Program program) {
		this.types = types;
		this.variables = new HashMap<>();
		createVariables();
		this.program = program;
	}
	
	public byte[] compile() {
		var def = ClassDef.create("fi.benjami.parserkit.minipl.Program", Access.PUBLIC);
		def.interfaces(Type.of(Runnable.class));
		def.addEmptyConstructor(Access.PUBLIC);
		
		var method = def.addMethod(Type.VOID, "run", Access.PUBLIC);
		method.add(initVariables()); // Initialize all variables with some default value
		method.add(compile(program.block()));
		method.add(Return.nothing());
		
		return def.compile();
	}
	
	private void createVariables() {
		for (var entry : types.varTypes().entrySet()) {
			variables.put(entry.getKey(), Variable.create(entry.getValue(), entry.getKey()));
		}
	}
	
	private Statement initVariables() {
		return block -> {
			for (var variable : variables.values()) {
				Constant defaultValue;
				if (variable.type().equals(Type.INT)) {
					defaultValue = Constant.of(0);
				} else if (variable.type().equals(Type.BOOLEAN)) {
					defaultValue = Constant.of(false);
				} else if (variable.type().equals(Type.STRING)) {
					defaultValue = Constant.of("");
				} else {
					throw new AssertionError();
				}
				block.add(variable.set(defaultValue));
			}
		};
	}
	
	private Statement compile(MiniPlNodes.Block node) {
		return block -> {
			for (var stmt : node.statements()) {
				block.add(compile(stmt));
			}
		};
	}
	
	private Statement compile(MiniPlNodes.Statement node) {
		return block -> {
			if (node instanceof MiniPlNodes.VarDeclaration varDecl) {
				if (varDecl.initialValue() != null) {
					var value = block.add(compile(varDecl.initialValue()));
					block.add(variables.get(varDecl.name()).set(value));
				}
			} else if (node instanceof MiniPlNodes.VarAssignment varAssign) {
				var value = block.add(compile(varAssign.value()));
				block.add(variables.get(varAssign.name()).set(value));
			} else if (node instanceof MiniPlNodes.BuiltinRead read) {
				var type = types.varTypes().get(read.variable());
				Value result;
				if (type.equals(Type.INT)) {
					result = block.add(MiniPlRuntime.TYPE.callStatic(Type.INT, "readInt"));
				} else {
					result = block.add(MiniPlRuntime.TYPE.callStatic(Type.STRING, "readString"));
				}
				block.add(variables.get(read.variable()).set(result));
			} else if (node instanceof MiniPlNodes.BuiltinPrint print) {
				var expr = block.add(compile(print.expr()));
				// Backend will automatically select correct print method based on expr type
				block.add(MiniPlRuntime.TYPE.callStatic(Type.VOID, "print", expr));
			} else if (node instanceof MiniPlNodes.IfBlock ifBlock) {
				var result = block.add(compile(ifBlock.condition()));
				var test = new IfBlock();
				test.branch(Condition.isTrue(result), inner -> {
					inner.add(compile(ifBlock.body()));
				});
				if (ifBlock.fallback() != null) {					
					test.fallback(inner -> {
						inner.add(compile(ifBlock.fallback()));
					});
				}
				block.add(test);
			} else if (node instanceof MiniPlNodes.ForBlock forBlock) {
				var counter = variables.get(forBlock.counter());
				var start = block.add(compile(forBlock.start()));
				var end = block.add(compile(forBlock.end()));
				block.add(counter.set(start));
				
				var body = Block.create();
				var loop = LoopBlock.whileLoop(body, Condition.lessOrEqual(counter, end));
				body.add(compile(forBlock.body()));
				
				// Increment counter
				var next = body.add(Arithmetic.add(counter, Constant.of(1)));
				body.add(counter.set(next));
				
				block.add(loop);
			} else {
				throw new AssertionError();
			}
		};
	}
	
	private Expression compile(MiniPlNodes.Expression node) {
		// TODO try to reduce duplicated code somehow
		return block -> {
			if (node instanceof MiniPlNodes.Group group) {
				return block.add(compile(group.expr()));
			} else if (node instanceof MiniPlNodes.LogicalNotExpr lNot) {
				var expr = block.add(compile(lNot.expr()));
				
				// TODO code4jvm: invert boolean
				var result = Variable.create(Type.BOOLEAN);
				var test = new IfBlock();
				test.branch(Condition.isFalse(expr), inner -> {
					inner.add(result.set(Constant.of(true)));
				});
				test.fallback(inner -> {
					inner.add(result.set(Constant.of(false)));
				});
				block.add(test);
				return result;
			} else if (node instanceof MiniPlNodes.EqualsExpr equals) {
				var lhs = block.add(compile(equals.lhs()));
				var rhs = block.add(compile(equals.rhs()));
				
				// TODO code4jvm: Condition -> Expression
				var result = Variable.create(Type.BOOLEAN);
				var test = new IfBlock();
				test.branch(Condition.equal(lhs, rhs), inner -> {
					inner.add(result.set(Constant.of(true)));
				});
				test.fallback(inner -> {
					inner.add(result.set(Constant.of(false)));
				});
				block.add(test);
				return result;
			} else if (node instanceof MiniPlNodes.LessThanExpr lessThan) {
				var lhs = block.add(compile(lessThan.lhs()));
				var rhs = block.add(compile(lessThan.rhs()));
				
				// TODO code4jvm: Condition -> Expression
				var result = Variable.create(Type.BOOLEAN);
				var test = new IfBlock();
				test.branch(Condition.lessThan(lhs, rhs), inner -> {
					inner.add(result.set(Constant.of(true)));
				});
				test.fallback(inner -> {
					inner.add(result.set(Constant.of(false)));
				});
				block.add(test);
				return result;
			} else if (node instanceof MiniPlNodes.AddExpr add) {
				var lhs = block.add(compile(add.lhs()));
				var rhs = block.add(compile(add.rhs()));
				return block.add(Arithmetic.add(lhs, rhs));
			} else if (node instanceof MiniPlNodes.SubtractExpr subtract) {
				var lhs = block.add(compile(subtract.lhs()));
				var rhs = block.add(compile(subtract.rhs()));
				return block.add(Arithmetic.subtract(lhs, rhs));
			} else if (node instanceof MiniPlNodes.MultiplyExpr multiply) {
				var lhs = block.add(compile(multiply.lhs()));
				var rhs = block.add(compile(multiply.rhs()));
				return block.add(Arithmetic.multiply(lhs, rhs));
			} else if (node instanceof MiniPlNodes.DivideExpr divide) {
				var lhs = block.add(compile(divide.lhs()));
				var rhs = block.add(compile(divide.rhs()));
				return block.add(Arithmetic.divide(lhs, rhs));
			} else if (node instanceof MiniPlNodes.Literal literal) {
				var value = literal.value();
				if (value instanceof MiniPlNodes.Constant constant) {
					var val = constant.value();
					// TODO code4jvm: Constant#parse(Object)
					if (val instanceof Integer integer) {
						return Constant.of(integer);
					} else if (val instanceof String str) {
						return Constant.of(str);
					} else if (val instanceof Boolean bool) {
						return Constant.of(bool);
					}
				} else if (value instanceof MiniPlNodes.VarReference varRef) {
					return variables.get(varRef.variable());
				}
			}
			throw new AssertionError(); // else: shouldn't happen
		};
	}
}
