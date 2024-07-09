package fi.benjami.code4jvm.lua.compiler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fi.benjami.code4jvm.lua.ir.LuaLocalVar;
import fi.benjami.code4jvm.lua.ir.LuaType;
import fi.benjami.code4jvm.lua.ir.LuaVariable;
import fi.benjami.code4jvm.lua.ir.TableField;
import fi.benjami.code4jvm.lua.ir.expr.LuaConstant;
import fi.benjami.code4jvm.lua.ir.expr.VariableExpr;
import fi.benjami.code4jvm.lua.ir.expr.FunctionDeclExpr.Upvalue;
import fi.benjami.code4jvm.lua.ir.stmt.LoopStmt;

public class LuaScope {
	
	public static LuaScope chunkRoot() {
		var scope = new LuaScope(null, true);
		var env = scope.declare("_ENV");
		scope.upvalues.put("_ENV", new Upvalue(env, null));
		return scope;
	}
	
	private final LuaScope parent;
	private final boolean functionRoot;
	
	private final Map<String, LuaLocalVar> locals;
	private final Map<String, Upvalue> upvalues;
	
	/**
	 * Reference to current loop or null, used for break'ing out of loop.
	 * TODO this is kinda hacky
	 */
	private final LoopRef currentLoop;
	
	private boolean hasVarargs;
	
	public LuaScope(LuaScope parent, boolean functionRoot, LoopRef currentLoop) {
		this.parent = parent;
		this.functionRoot = functionRoot;
		this.locals = new HashMap<>();
		this.currentLoop = currentLoop;
		if (functionRoot) {
			this.upvalues = new HashMap<>();
		} else {
			this.upvalues = parent.upvalues;
		}
	}
	
	public LuaScope(LuaScope parent, boolean functionRoot) {
		this(parent, functionRoot, functionRoot ? null : parent.currentLoop);
	}
	
	public LuaLocalVar declare(String name) {
		// Lua spec allows shadowing local variables within same scope...
		// TODO emit warnings for shadowing?
		var variable = new LuaLocalVar(name);
		locals.put(name, variable);
		return variable;
	}
	
	public LuaVariable resolve(String name) {
		var result = resolveLocal(name);
		if (result != null) {
			// Local variable or upvalue
			if (result.isUpvalue()) {
				// Upvalue: record it and create a local variable
				var inside = new LuaLocalVar(name);
				locals.put(name, inside);
				var outside = result.variable();
				upvalues.put(name, new Upvalue(inside, outside));
				return inside;
			} else {
				return result.variable(); // Local variable
			}
		} else {			
			// Neither local variable or upvalue; take a look at _ENV table
			var env = resolve("_ENV");
			return new TableField(new VariableExpr(env), new LuaConstant(name, LuaType.STRING));
		}
	}
	
	private record ResolveResult(LuaLocalVar variable, boolean isUpvalue) {}
	
	private ResolveResult resolveLocal(String name) {
		var local = locals.get(name);
		if (local != null) {
			return new ResolveResult(local, false);
		} else {
			if (parent != null) {
				var result = parent.resolveLocal(name);
				// Mark results that cross function boundaries as upvalues
				return result != null && functionRoot ? new ResolveResult(result.variable(), true) : result;
			} else {
				return null; // Reached root without finding variable
			}
		}
	}
	
	public List<Upvalue> upvalues() {
		return new ArrayList<>(upvalues.values());
	}
	
	public LoopRef currentLoop() {
		return currentLoop;
	}
	
	public void addVarargs() {
		assert !hasVarargs;
		hasVarargs = true;
	}
	
	public boolean hasVarargs() {
		return hasVarargs;
	}
	
}
