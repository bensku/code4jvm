package fi.benjami.code4jvm.lua.compiler;

import java.util.IdentityHashMap;
import java.util.Map;

import fi.benjami.code4jvm.Variable;
import fi.benjami.code4jvm.lua.ir.LuaLocalVar;
import fi.benjami.code4jvm.lua.ir.LuaType;
import fi.benjami.code4jvm.lua.ir.LuaVariable;

public class LuaContext {
	
	private final Map<LuaLocalVar, LuaType> typeTable;
	private final Map<LuaLocalVar, Variable> variables;
	
	private LuaType[] returnTypes;
	
	LuaContext() {
		this.typeTable = new IdentityHashMap<>();
		this.variables = new IdentityHashMap<>();
	}
	
	public void recordType(LuaVariable variable, LuaType type) {
		if (variable instanceof LuaLocalVar localVar) {			
			var oldType = typeTable.get(variable);
			if (oldType == null) {
				typeTable.put(localVar, type);
			} else if (!oldType.equals(type)) {
				typeTable.put(localVar, LuaType.UNKNOWN); // Mixed types
			} // else: same type again, do nothing
		}
		// TODO support non-local variable optimization
	}
	
	public void addFunctionArg(LuaLocalVar arg, Variable variable) {
		variables.put(arg, variable);
	}
	
	public LuaType variableType(LuaVariable variable) {
		if (variable instanceof LuaLocalVar) {			
			assert typeTable.containsKey(variable);
			return typeTable.get(variable);
		} else {
			// TODO non-local variable optimization
			return LuaType.UNKNOWN;
		}
	}
	
	public Variable resolveLocalVar(LuaLocalVar variable) {
		var backingVar = variables.get(variable);
		if (backingVar == null) {
			var type = typeTable.get(variable);
			backingVar = Variable.create(type.backingType());
			variables.put(variable, backingVar);
		}
		return backingVar;
	}
	
	public void returnTypes(LuaType... types) {
		if (returnTypes == null) {
			returnTypes = types;
		} else {
			int commonEnd;
			if (types.length > returnTypes.length) {
				// New return has more values than existing returns
				commonEnd = returnTypes.length;
				returnTypes = types;
				for (var i = commonEnd; i < returnTypes.length; i++) {
					// Types of values that might not be returned are unknown
					// (unless they are always known to be nil)
					if (!returnTypes[i].equals(LuaType.NIL)) {						
						returnTypes[i] = LuaType.UNKNOWN;
					}
				}
			} else {
				// This return has at least the same amount of values as previous ones
				commonEnd = types.length;
				for (var i = commonEnd; i < returnTypes.length; i++) {
					// Values that are not always returned are implicitly nil-able
					if (!returnTypes[i].equals(LuaType.NIL)) {						
						returnTypes[i] = LuaType.UNKNOWN;
					}
				}
			}
			
			for (var i = 0; i < commonEnd; i++) {
				// Caller does not know types of return values if there are many options
				// TODO add support for nullable types to do this in more fine-grained way
				if (!returnTypes[i].equals(types[i])) {
					returnTypes[i] = LuaType.UNKNOWN;
				}
			}
		}
	}
	
	public LuaType[] returnTypes() {
		assert returnTypes != null;
		return returnTypes;
	}
}
