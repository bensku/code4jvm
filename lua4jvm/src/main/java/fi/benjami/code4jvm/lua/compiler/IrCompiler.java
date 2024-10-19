package fi.benjami.code4jvm.lua.compiler;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import fi.benjami.code4jvm.lua.ir.DebugInfoNode;
import fi.benjami.code4jvm.lua.ir.IrNode;
import fi.benjami.code4jvm.lua.ir.LuaBlock;
import fi.benjami.code4jvm.lua.ir.LuaLocalVar;
import fi.benjami.code4jvm.lua.ir.LuaVariable;
import fi.benjami.code4jvm.lua.ir.TableField;
import fi.benjami.code4jvm.lua.ir.expr.ArithmeticExpr;
import fi.benjami.code4jvm.lua.ir.expr.CompareExpr;
import fi.benjami.code4jvm.lua.ir.expr.FunctionCallExpr;
import fi.benjami.code4jvm.lua.ir.expr.FunctionDeclExpr;
import fi.benjami.code4jvm.lua.ir.expr.LengthExpr;
import fi.benjami.code4jvm.lua.ir.expr.LogicalExpr;
import fi.benjami.code4jvm.lua.ir.expr.LuaConstant;
import fi.benjami.code4jvm.lua.ir.expr.NegateExpr;
import fi.benjami.code4jvm.lua.ir.expr.StringConcatExpr;
import fi.benjami.code4jvm.lua.ir.expr.TableInitExpr;
import fi.benjami.code4jvm.lua.ir.expr.VariableExpr;
import fi.benjami.code4jvm.lua.ir.stmt.IfBlockStmt;
import fi.benjami.code4jvm.lua.ir.stmt.IteratorForStmt;
import fi.benjami.code4jvm.lua.ir.stmt.LoopBreakStmt;
import fi.benjami.code4jvm.lua.ir.stmt.LoopStmt;
import fi.benjami.code4jvm.lua.ir.stmt.ReturnStmt;
import fi.benjami.code4jvm.lua.ir.stmt.SetVariablesStmt;
import fi.benjami.code4jvm.lua.parser.LuaBaseVisitor;
import fi.benjami.code4jvm.lua.parser.LuaParser.ArgsContext;
import fi.benjami.code4jvm.lua.parser.LuaParser.ArithmeticOpContext;
import fi.benjami.code4jvm.lua.parser.LuaParser.ArrayAccessContext;
import fi.benjami.code4jvm.lua.parser.LuaParser.AssignLocalContext;
import fi.benjami.code4jvm.lua.parser.LuaParser.AssignVarContext;
import fi.benjami.code4jvm.lua.parser.LuaParser.AttnamelistContext;
import fi.benjami.code4jvm.lua.parser.LuaParser.AttribContext;
import fi.benjami.code4jvm.lua.parser.LuaParser.BitwiseOpContext;
import fi.benjami.code4jvm.lua.parser.LuaParser.BlockContext;
import fi.benjami.code4jvm.lua.parser.LuaParser.BreakStmtContext;
import fi.benjami.code4jvm.lua.parser.LuaParser.ChunkContext;
import fi.benjami.code4jvm.lua.parser.LuaParser.CompareOpContext;
import fi.benjami.code4jvm.lua.parser.LuaParser.CountedForLoopContext;
import fi.benjami.code4jvm.lua.parser.LuaParser.DoEndBlockContext;
import fi.benjami.code4jvm.lua.parser.LuaParser.EmptyContext;
import fi.benjami.code4jvm.lua.parser.LuaParser.ExplistContext;
import fi.benjami.code4jvm.lua.parser.LuaParser.FalseLiteralContext;
import fi.benjami.code4jvm.lua.parser.LuaParser.FieldContext;
import fi.benjami.code4jvm.lua.parser.LuaParser.FieldlistContext;
import fi.benjami.code4jvm.lua.parser.LuaParser.FieldsepContext;
import fi.benjami.code4jvm.lua.parser.LuaParser.ForInLoopContext;
import fi.benjami.code4jvm.lua.parser.LuaParser.FuncbodyContext;
import fi.benjami.code4jvm.lua.parser.LuaParser.FunctionCallStmtContext;
import fi.benjami.code4jvm.lua.parser.LuaParser.FunctionCall_Context;
import fi.benjami.code4jvm.lua.parser.LuaParser.FunctionContext;
import fi.benjami.code4jvm.lua.parser.LuaParser.FunctionDefExp_Context;
import fi.benjami.code4jvm.lua.parser.LuaParser.FunctioncallContext;
import fi.benjami.code4jvm.lua.parser.LuaParser.FunctiondefContext;
import fi.benjami.code4jvm.lua.parser.LuaParser.GotoLabelContext;
import fi.benjami.code4jvm.lua.parser.LuaParser.GotoStmtContext;
import fi.benjami.code4jvm.lua.parser.LuaParser.GroupContext;
import fi.benjami.code4jvm.lua.parser.LuaParser.IfBlockContext;
import fi.benjami.code4jvm.lua.parser.LuaParser.LabelContext;
import fi.benjami.code4jvm.lua.parser.LuaParser.LocalFunctionContext;
import fi.benjami.code4jvm.lua.parser.LuaParser.LogicalOpContext;
import fi.benjami.code4jvm.lua.parser.LuaParser.NamelistContext;
import fi.benjami.code4jvm.lua.parser.LuaParser.NilLiteralContext;
import fi.benjami.code4jvm.lua.parser.LuaParser.NumberLiteralContext;
import fi.benjami.code4jvm.lua.parser.LuaParser.ParlistContext;
import fi.benjami.code4jvm.lua.parser.LuaParser.PrefixExp_Context;
import fi.benjami.code4jvm.lua.parser.LuaParser.RepeatLoopContext;
import fi.benjami.code4jvm.lua.parser.LuaParser.ReturnContext;
import fi.benjami.code4jvm.lua.parser.LuaParser.StringConcatContext;
import fi.benjami.code4jvm.lua.parser.LuaParser.StringLiteralContext;
import fi.benjami.code4jvm.lua.parser.LuaParser.TableAccessContext;
import fi.benjami.code4jvm.lua.parser.LuaParser.TableConstructor_Context;
import fi.benjami.code4jvm.lua.parser.LuaParser.TableconstructorContext;
import fi.benjami.code4jvm.lua.parser.LuaParser.TrueLiteralContext;
import fi.benjami.code4jvm.lua.parser.LuaParser.UnaryOpContext;
import fi.benjami.code4jvm.lua.parser.LuaParser.UnopContext;
import fi.benjami.code4jvm.lua.parser.LuaParser.VarContext;
import fi.benjami.code4jvm.lua.parser.LuaParser.VarReferenceContext;
import fi.benjami.code4jvm.lua.parser.LuaParser.VarargsContext;
import fi.benjami.code4jvm.lua.parser.LuaParser.VarlistContext;
import fi.benjami.code4jvm.lua.parser.LuaParser.WhileLoopContext;

public class IrCompiler extends LuaBaseVisitor<IrNode> {
	
	private final String moduleName;
	private final Deque<LuaScope> scopes;
	
	private int lastLine;
	
	public IrCompiler(String moduleName, LuaScope rootScope) {
		this.moduleName = moduleName;
		this.scopes = new ArrayDeque<>();
		scopes.push(rootScope);
	}
	
	private LuaScope currentScope() {
		return scopes.peekFirst();
	}
	
	private void pushScope(LuaScope scope) {
		scopes.push(scope);
	}
	
	private void popScope() {
		scopes.pop();
	}
	
	@Override
	public IrNode visit(ParseTree tree) {
		if (tree instanceof ParserRuleContext ctx) {
			var line = ctx.getStart().getLine();
			if (line != lastLine) {
				// Line number changed, make sure to record it
				assert line > lastLine;
				return new DebugInfoNode(line, super.visit(tree));
			}
		}
		return super.visit(tree);
	}
	
	@Override
	public LuaBlock visitChunk(ChunkContext ctx) {
		return visitBlock(ctx.block());
	}
	
	@Override
	public LuaBlock visitBlock(BlockContext ctx) {
		return new LuaBlock(ctx.stat().stream()
				.map(this::visit)
				.filter(Objects::nonNull)
				.toList());
	}
	
	@Override
	public IrNode visitEmpty(EmptyContext ctx) {
		return null;
	}
	
	@Override
	public IrNode visitAssignVar(AssignVarContext ctx) {
		var targets = ctx.targets.var().stream()
				.map(this::visitVar)
				.map(VariableExpr::source)
				.toList();
		var sources = ctx.values.exp().stream()
				.map(this::visit)
				.toList();
		return new SetVariablesStmt(targets, sources);
	}
	
	@Override
	public IrNode visitFunctionCallStmt(FunctionCallStmtContext ctx) {
		return visit(ctx.functioncall());
	}
	
	@Override
	public IrNode visitGotoLabel(GotoLabelContext ctx) {
		throw new UnsupportedOperationException("goto");
	}
	
	@Override
	public IrNode visitBreakStmt(BreakStmtContext ctx) {
		var loop = currentScope().currentLoop();
		if (loop == null) {
			// TODO semantic error: break outside loop
			throw new AssertionError();
		}
		return new LoopBreakStmt(loop);
	}
	
	@Override
	public IrNode visitGotoStmt(GotoStmtContext ctx) {
		throw new UnsupportedOperationException("goto");
	}

	@Override
	public IrNode visitDoEndBlock(DoEndBlockContext ctx) {
		pushScope(new LuaScope(currentScope(), false));
		var block = visitBlock(ctx.block());
		popScope();
		return block;
	}

	@Override
	public IrNode visitWhileLoop(WhileLoopContext ctx) {
		var ref = new LoopRef();
		var condition = visit(ctx.condition);
		pushScope(new LuaScope(currentScope(), false, ref));
		var body = visitBlock(ctx.block());
		popScope();
		return new LoopStmt(condition, body, LoopStmt.Kind.WHILE, ref);
	}

	@Override
	public IrNode visitRepeatLoop(RepeatLoopContext ctx) {
		var ref = new LoopRef();
		var condition = visit(ctx.condition);
		pushScope(new LuaScope(currentScope(), false, ref));
		var body = visitBlock(ctx.block());
		popScope();
		return new LoopStmt(condition, body, LoopStmt.Kind.REPEAT_UNTIL, ref);
	}

	@Override
	public IrNode visitIfBlock(IfBlockContext ctx) {
		var branches = new ArrayList<IfBlockStmt.Branch>(ctx.exp().size());
		// if and optional elseif clauses
		for (var i = 0; i < ctx.exp().size(); i++) {
			var condition = visit(ctx.exp(i));
			pushScope(new LuaScope(currentScope(), false));
			var body = visitBlock(ctx.block(i));
			popScope();
			branches.add(new IfBlockStmt.Branch(condition, body));
		}
		// optional else/fallback clause
		LuaBlock fallback = null;
		if (ctx.block().size() > ctx.exp().size()) {
			pushScope(new LuaScope(currentScope(), false));
			fallback = visitBlock(ctx.block(ctx.block().size() - 1));
			popScope();
		}
		return new IfBlockStmt(branches, fallback);
	}

	@Override
	public IrNode visitCountedForLoop(CountedForLoopContext ctx) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IrNode visitForInLoop(ForInLoopContext ctx) {
		var ref = new LoopRef();
		var iterable = ctx.iterable.exp().stream()
				.map(this::visit)
				.toList();
		pushScope(new LuaScope(currentScope(), false, ref));
		var innerScope = currentScope();
		var loopVars = ctx.entries.Name().stream()
				.map(TerminalNode::getText)
				.map(innerScope::declare)
				.toList();
		var body = visitBlock(ctx.block());
		popScope();
		return new IteratorForStmt(body, ref, loopVars, iterable);
	}

	@Override
	public IrNode visitFunction(FunctionContext ctx) {
		// Convert function name
		var target = new VariableExpr(currentScope().resolve(ctx.Name(0).getText()));
		for (var i = 1; i < ctx.Name().size(); i++) {
			target = new VariableExpr(new TableField(target, new LuaConstant(ctx.Name(i))));
		}
		// TODO incorporate the full name as part of function name
		var function = visitFuncbody(ctx.Name(ctx.Name().size() - 1).getText(), ctx.funcbody(), ctx.oopPart != null);
		return new SetVariablesStmt(List.of(target.source()), List.of(function), false);
	}

	@Override
	public IrNode visitLocalFunction(LocalFunctionContext ctx) {
		var name = ctx.Name().getText();
		var function = visitFuncbody(name, ctx.funcbody(), false);
		return new SetVariablesStmt(List.of(currentScope().declare(name)), List.of(function), false);
	}

	@Override
	public IrNode visitAssignLocal(AssignLocalContext ctx) {
		var scope = currentScope();
		var targets = ctx.names.Name().stream()
				.map(TerminalNode::getText)
				.map(scope::declare)
				.toList();
		var sources = ctx.values.exp().stream()
				.map(this::visit)
				.toList();
		return new SetVariablesStmt(targets, sources);
	}

	@Override
	public IrNode visitReturn(ReturnContext ctx) {
		if (ctx.values == null) {
			return new ReturnStmt(List.of());
		}
		return new ReturnStmt(ctx.values.exp().stream()
				.map(this::visit)
				.toList());
	}

	@Override
	public IrNode visitAttnamelist(AttnamelistContext ctx) {
		throw new AssertionError();
	}

	@Override
	public IrNode visitAttrib(AttribContext ctx) {
		throw new AssertionError();
	}

	@Override
	public IrNode visitLabel(LabelContext ctx) {
		throw new AssertionError();
	}

	@Override
	public IrNode visitVarlist(VarlistContext ctx) {
		throw new AssertionError();
	}
	
	@Override
	public VariableExpr visitVar(VarContext ctx) {
		var scope = currentScope();
		LuaVariable source;
		if (ctx.exp() != null) { // table[key]
			source = new TableField(visit(ctx.table), visit(ctx.exp()));
		} else if (ctx.prefixexp() != null) { // table.key
			source = new TableField(visit(ctx.table), new LuaConstant(ctx.Name().getText()));
		} else { // varname
			source = scope.resolve(ctx.Name().getText());
		}
		return new VariableExpr(source);
	}

	@Override
	public IrNode visitNamelist(NamelistContext ctx) {
		throw new AssertionError();
	}

	@Override
	public IrNode visitExplist(ExplistContext ctx) {
		throw new AssertionError();
	}

	@Override
	public IrNode visitTableConstructor_(TableConstructor_Context ctx) {
		return visitTableconstructor(ctx.tableconstructor());
	}

	@Override
	public IrNode visitPrefixExp_(PrefixExp_Context ctx) {
		return visit(ctx.prefixexp());
	}

	@Override
	public IrNode visitNilLiteral(NilLiteralContext ctx) {
		return new LuaConstant(null);
	}

	@Override
	public IrNode visitLogicalOp(LogicalOpContext ctx) {
		var type = ctx.op.getText().equals("and") ? LogicalExpr.Kind.AND : LogicalExpr.Kind.OR;
		return new LogicalExpr(visit(ctx.lhs), type, visit(ctx.rhs));
	}

	@Override
	public IrNode visitTrueLiteral(TrueLiteralContext ctx) {
		return new LuaConstant(true);
	}

	@Override
	public IrNode visitUnaryOp(UnaryOpContext ctx) {
		return switch (ctx.unop().getText()) {
		case "-" -> new NegateExpr(visit(ctx.exp()));
		case "not" -> throw new UnsupportedOperationException();
		case "#" -> new LengthExpr(visit(ctx.exp()));
		case "~" -> throw new UnsupportedOperationException();
		default -> throw new AssertionError();
		};
	}

	@Override
	public IrNode visitCompareOp(CompareOpContext ctx) {
		var kind = switch (ctx.op.getText()) {
		case "<" -> CompareExpr.Kind.LESS_THAN;
		case ">" -> CompareExpr.Kind.MORE_THAN;
		case "<=" -> CompareExpr.Kind.LESS_OR_EQUAL;
		case ">=" -> CompareExpr.Kind.MORE_OR_EQUAL;
		case "~=" -> CompareExpr.Kind.NOT_EQUAL;
		case "==" -> CompareExpr.Kind.EQUAL;
		default -> throw new AssertionError();
		};
		return new CompareExpr(visit(ctx.lhs), kind, visit(ctx.rhs));
	}

	@Override
	public IrNode visitFalseLiteral(FalseLiteralContext ctx) {
		return new LuaConstant(false);
	}

	@Override
	public IrNode visitStringLiteral(StringLiteralContext ctx) {
		var text = ctx.LiteralString().getText();
		return new LuaConstant(text.substring(1, text.length() - 1));
	}

	@Override
	public IrNode visitFunctionDefExp_(FunctionDefExp_Context ctx) {
		return visitFunctiondef(ctx.functiondef());
	}

	@Override
	public IrNode visitArithmeticOp(ArithmeticOpContext ctx) {
		var kind = switch (ctx.op.getText()) {
		case "^" -> ArithmeticExpr.Kind.POWER;
		case "*" -> ArithmeticExpr.Kind.MULTIPLY;
		case "/" -> ArithmeticExpr.Kind.DIVIDE;
		case "%" -> ArithmeticExpr.Kind.MODULO;
		case "//" -> ArithmeticExpr.Kind.FLOOR_DIVIDE;
		case "+" -> ArithmeticExpr.Kind.ADD;
		case "-" -> ArithmeticExpr.Kind.SUBTRACT;
		default -> throw new AssertionError();
		};
		return new ArithmeticExpr(visit(ctx.lhs), kind, visit(ctx.rhs));
	}

	@Override
	public IrNode visitBitwiseOp(BitwiseOpContext ctx) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IrNode visitVarargs(VarargsContext ctx) {
		if (!currentScope().hasVarargs()) {
			// TODO semantic error: function has no varargs
			throw new AssertionError();
		}
		
		return new VariableExpr(LuaLocalVar.VARARGS);
	}

	@Override
	public IrNode visitStringConcat(StringConcatContext ctx) {
		return new StringConcatExpr(List.of(visit(ctx.lhs), visit(ctx.rhs)));
	}

	@Override
	public IrNode visitNumberLiteral(NumberLiteralContext ctx) {
		var value = Double.valueOf(ctx.Numeral().getText());
		// Use Math.rint() to handle very large doubles safely
		return Math.rint(value) == value ? new LuaConstant(value.intValue()) : new LuaConstant(value);
	}

	@Override
	public IrNode visitTableAccess(TableAccessContext ctx) {
		return new VariableExpr(new TableField(visit(ctx.table), new LuaConstant(ctx.key.getText())));
	}

	@Override
	public IrNode visitArrayAccess(ArrayAccessContext ctx) {
		return new VariableExpr(new TableField(visit(ctx.table), visit(ctx.key)));
	}

	@Override
	public IrNode visitFunctionCall_(FunctionCall_Context ctx) {
		// FIXME duplicated code with visitFunctioncall
		var function = visit(ctx.func);
		var irArgs = new ArrayList<IrNode>();
		if (ctx.oopPart != null) {
			// Syntactic sugar for "OOP"; table:func(...) -> table.func(table, ...)
			irArgs.add(function); // i.e. the table is first argument
			function = new VariableExpr(new TableField(function, new LuaConstant(ctx.oopPart.getText())));
		}
		// Rest of the arguments
		if (ctx.args().tableconstructor() != null) { // No parentheses, one table constructor
			irArgs.add(visitTableconstructor(ctx.args().tableconstructor()));
		} else if (ctx.args().LiteralString() != null) { // No parentheses, string literal
			irArgs.add(new LuaConstant((String) ctx.args().LiteralString().getText()));
		} else if (ctx.args().explist() != null) { // Parentheses, 1 or more arguments
			irArgs.addAll(ctx.args().explist().exp().stream()
					.map(this::visit).toList());
		} // else: 0 arguments
		return new FunctionCallExpr(function, irArgs);
	}

	@Override
	public IrNode visitVarReference(VarReferenceContext ctx) {
		return new VariableExpr(currentScope().resolve(ctx.Name().getText()));
	}

	@Override
	public IrNode visitGroup(GroupContext ctx) {
		return visit(ctx.exp());
	}

	@Override
	public IrNode visitFunctioncall(FunctioncallContext ctx) {
		var function = visit(ctx.func);
		var irArgs = new ArrayList<IrNode>();
		if (ctx.oopPart != null) {
			// Syntactic sugar for "OOP"; table:func(...) -> table.func(table, ...)
			irArgs.add(function); // i.e. the table is first argument
			function = new VariableExpr(new TableField(function, new LuaConstant(ctx.oopPart.getText())));
		}
		// Rest of the arguments
		if (ctx.args().tableconstructor() != null) { // No parentheses, one table constructor
			irArgs.add(visitTableconstructor(ctx.args().tableconstructor()));
		} else if (ctx.args().LiteralString() != null) { // No parentheses, string literal
			irArgs.add(new LuaConstant((String) ctx.args().LiteralString().getText()));
		} else if (ctx.args().explist() != null) { // Parentheses, 1 or more arguments
			irArgs.addAll(ctx.args().explist().exp().stream()
					.map(this::visit).toList());
		} // else: 0 arguments
		return new FunctionCallExpr(function, irArgs);
	}

	@Override
	public IrNode visitArgs(ArgsContext ctx) {
		throw new AssertionError();
	}

	@Override
	public IrNode visitFunctiondef(FunctiondefContext ctx) {
		return visitFuncbody("anonymous", ctx.funcbody(), false);
	}

	public IrNode visitFuncbody(String name, FuncbodyContext ctx, boolean addSelfArg) {
		pushScope(new LuaScope(currentScope(), true));
		var scope = currentScope();
		List<LuaLocalVar> args;
		if (ctx.argList != null) {
			var names = ctx.argList.names;
			if (names != null) {				
				args = ctx.argList.names.Name().stream()
						.map(TerminalNode::getText)
						.map(scope::declare)
						.collect(Collectors.toCollection(ArrayList::new));
			} else {
				args = new ArrayList<>(); // Only varargs?
			}
			if (ctx.argList.rest != null) {
				currentScope().addVarargs(); // Allow usage of varargs in this function
				args.add(LuaLocalVar.VARARGS);
			}
			assert args.size() > 0;
		} else {
			args = new ArrayList<>();
		}
		if (addSelfArg) {
			throw new UnsupportedOperationException();
		}
		var body = visitBlock(ctx.block());	
		popScope();
		return new FunctionDeclExpr(moduleName, name, scope.upvalues(), args, body);
	}

	@Override
	public IrNode visitParlist(ParlistContext ctx) {
		throw new AssertionError();
	}

	@Override
	public IrNode visitTableconstructor(TableconstructorContext ctx) {
		if (ctx.fieldlist() == null) {
			return new TableInitExpr(List.of()); // Empty table
		}
		var fields = ctx.fieldlist().field();
		var entries = new ArrayList<TableInitExpr.Entry>(fields.size());
		var arrayIndex = 1d;
		for (var field : fields) {
			IrNode key;
			if (field.key != null) {
				key = new LuaConstant(field.key.getText());
			} else if (field.keyExp != null) {
				key = visit(field.keyExp);
			} else {
				key = new LuaConstant(arrayIndex++);
			}
			entries.add(new TableInitExpr.Entry(key, visit(field.value)));
		}
		return new TableInitExpr(entries);
	}

	@Override
	public IrNode visitFieldlist(FieldlistContext ctx) {
		throw new AssertionError();
	}

	@Override
	public IrNode visitField(FieldContext ctx) {
		throw new AssertionError();
	}

	@Override
	public IrNode visitFieldsep(FieldsepContext ctx) {
		throw new AssertionError();
	}

	@Override
	public IrNode visitUnop(UnopContext ctx) {
		throw new AssertionError();
	}

}
