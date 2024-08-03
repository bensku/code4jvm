package fi.benjami.code4jvm.lua.compiler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import fi.benjami.code4jvm.Constant;
import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.Variable;
import fi.benjami.code4jvm.lua.LuaVm;
import fi.benjami.code4jvm.lua.ir.LuaLocalVar;
import fi.benjami.code4jvm.lua.ir.LuaType;
import fi.benjami.code4jvm.lua.ir.LuaVariable;
import fi.benjami.code4jvm.lua.ir.TableField;
import fi.benjami.code4jvm.lua.ir.expr.LuaConstant;

public class LuaContext {
	
	public static LuaContext forFunction(LuaVm vm, LuaType.Function type, boolean truncateReturn, LuaType... argTypes) {
		// Init scope with upvalue types
		var ctx = new LuaContext(vm, truncateReturn);
		for (var upvalue : type.upvalues()) {
			ctx.recordType(upvalue.variable(), upvalue.type());
		}

		// Add types of function arguments
		// Missing arguments are allowed and treated as nil
		var normalArgs = type.acceptedArgs().size();
		if (type.isVarargs()) {
			normalArgs--;
			ctx.recordType(LuaLocalVar.VARARGS, LuaType.UNKNOWN); // No type analysis for these yet
		}
		var acceptedArgs = type.acceptedArgs();
		for (var i = 0; i < normalArgs; i++) {
			if (argTypes.length > i) {
				ctx.recordType(acceptedArgs.get(i), argTypes[i]);
			} else {
				ctx.recordType(acceptedArgs.get(i), LuaType.NIL);
			}
		}
		
		// Compute types of local variables and the return type
		type.body().outputType(ctx);
		return ctx;
	}
	
	/**
	 * Local variable type table. When variables are written to, their types
	 * are recorded here.
	 */
	private final Map<LuaLocalVar, LuaType> typeTable;
	
	/**
	 * Local variables mapped to code4jvm's variables.
	 */
	private final Map<LuaLocalVar, Variable> variables;
	
	/**
	 * Data given to JVM when the function is loaded as a hidden class.
	 * This is used for creating constants of arbitrary kind, which can then
	 * be used at e.g. invokedynamic call sites.
	 */
	private final List<Object> classData;
	
	/**
	 * Cache for IR nodes.
	 */
	private final Map<Object, Object> cache;
	
	private final boolean truncateReturn;
	
	private LuaType[] returnTypes;
	
	private boolean allowSpread;
	
	/**
	 * The Lua VM that 'owns' this context.
	 */
	private final LuaVm owner;
	private final Constant ownerConstant;
	
	public LuaContext(LuaVm owner, boolean truncateReturn) {
		assert owner != null;
		this.typeTable = new IdentityHashMap<>();
		this.variables = new IdentityHashMap<>();
		this.classData = new ArrayList<>();
		this.cache = new IdentityHashMap<>();
		this.truncateReturn = truncateReturn;
		this.owner = owner;
		this.ownerConstant = addClassData(owner);
	}
	
	public void recordType(LuaVariable variable, LuaType type) {
		if (variable instanceof LuaLocalVar localVar) {
			var oldType = typeTable.get(variable);
			if (oldType == null) {
				typeTable.put(localVar, type);
			} else if (!oldType.equals(type)) {
				typeTable.put(localVar, LuaType.UNKNOWN); // Mixed types
			} // else: same type again, do nothing
		} else if (variable instanceof TableField tableField) {
			var tableType = tableField.table().outputType(this);
			if (tableType instanceof LuaType.Shape shape) {
				if (tableField.field() instanceof LuaConstant constant && constant.value() instanceof String str) {
					// Key is known, add it to shape with given type
					shape.amend(str, type);
				} else {
					// Key is not known until runtime, but we DO know the type!
					shape.amendUnknown(); // TODO use the type?
				}
			}
		}
	}
	
	public void addFunctionArg(LuaLocalVar arg, Variable variable) {
		variables.put(arg, variable);
	}
	
	public LuaType variableType(LuaVariable variable) {
		if (variable instanceof LuaLocalVar) {
			assert typeTable.containsKey(variable) : variable;
			return typeTable.get(variable);
		} else if (variable instanceof TableField tableField
				&& tableField.table().outputType(this) instanceof LuaType.Shape shape
				&& tableField.field() instanceof LuaConstant key
				&& key.value() instanceof String str) {
			// Known table shape and key -> we might know the type!
			return shape.types().getType(str);
		} else {
			return LuaType.UNKNOWN;
		}
	}
	
	public Variable resolveLocalVar(LuaLocalVar variable) {
		var backingVar = variables.get(variable);
		if (backingVar == null) {
			var type = typeTable.get(variable);
			backingVar = Variable.create(type.backingType(), variable.name());
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
	
	public LuaType returnType() {
		assert returnTypes != null;
		if (returnTypes.length == 0) {
			return LuaType.NIL;
		} else if (returnTypes.length == 1 || truncateReturn) {
			return returnTypes[0];
		} else {
			// JVM has no multiple returns, emulate them with tuples
			return LuaType.tuple(returnTypes);
		}
	}
	
	public Constant addClassData(Object value) {
		return addClassData(value, Type.of(value.getClass()));
	}
	
	public Constant addClassData(Object value, Type type) {
		var index = classData.size();
		classData.add(value);
		return Constant.dynamic(type, ClassData.BOOTSTRAP, Constant.of(index));
	}
	
	public Object[] allClassData() {
		return classData.toArray();
	}
	
	public <T> T cached(Object key, T value) {
		cache.put(key, value);
		return value;
	}
	
	public Object getCache(Object key) {
		return cache.get(key);
	}
	
	public boolean truncateReturn() {
		return truncateReturn;
	}
	
	public void setAllowSpread(boolean value) {
		this.allowSpread = value;
	}
	
	public boolean allowSpread() {
		return allowSpread;
	}
	
	public LuaVm owner() {
		return owner;
	}
	
	public Constant ownerConstant() {
		return ownerConstant;
	}

}
