package fi.benjami.code4jvm.lua.ir;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.Value;
import fi.benjami.code4jvm.lua.compiler.CompiledFunction;
import fi.benjami.code4jvm.lua.compiler.LuaContext;

public interface LuaType {

	class Simple implements LuaType {
		
		private final String name;
		private final Type backingType;
		
		private Simple(String name, Type backingType) {
			this.name = name;
			this.backingType = backingType;
		}

		@Override
		public String name() {
			return name;
		}
		
		@Override
		public Type backingType() {
			return backingType;
		}
	}
	
	class Tuple implements LuaType {
		
		private final LuaType[] types;
		
		private Tuple(LuaType[] types) {
			this.types = types;
		}
		
		public LuaType[] types() {
			return types;
		}

		@Override
		public String name() {
			return "tuple";
		}

		@Override
		public Type backingType() {
			// TODO specialized tuples?
			return Type.OBJECT.array(1);
		}
		
		@Override
		public int hashCode() {
			return Arrays.hashCode(types);
		}
		
		@Override
		public boolean equals(Object o) {
			return o instanceof Tuple tuple && Arrays.equals(tuple.types, types);
		}
	}
	
	class Function implements LuaType {
		
		private final UpvalueTemplate[] upvalues;
		private final LuaLocalVar[] acceptedArgs;
		private final LuaBlock body;
		
		private final Map<LuaType.Tuple, CompiledFunction> specializations;
		
		private Function(UpvalueTemplate[] upvalues, LuaLocalVar[] args, LuaBlock body) {
			this.upvalues = upvalues;
			this.acceptedArgs = args;
			this.body = body;
			this.specializations = new HashMap<>();
		}
		
		public LuaBlock body() {
			return body;
		}
		
		public UpvalueTemplate[] upvalues() {
			return upvalues;
		}
		
		public LuaLocalVar[] acceptedArgs() {
			return acceptedArgs;
		}
		
		public Map<LuaType.Tuple, CompiledFunction> specializations() {
			return specializations;
		}
		
		public LuaContext newContext(LuaType... argTypes) {
			// Init scope with upvalue types
			var ctx = new LuaContext();
			for (var upvalue : upvalues) {
				ctx.recordType(upvalue.variable(), upvalue.type());
			}

			// Add types of function arguments
			// Missing arguments are allowed and treated as nil
			for (var i = 0; i < acceptedArgs.length; i++) {
				if (argTypes.length > i) {				
					ctx.recordType(acceptedArgs[i], argTypes[i]);
				} else {
					ctx.recordType(acceptedArgs[i], LuaType.NIL);
				}
			}
			
			// Compute types of local variables and the return type
			body.outputType(ctx);
			return ctx;
		}

		@Override
		public String name() {
			return "function";
		}

		@Override
		public Type backingType() {
			// TODO make sure this doesn't break function call specialization
			return Type.of(Object.class);
		}
	}
	
	// Lua standard types
	static final LuaType NIL = new Simple("nil", Type.OBJECT);
	static final LuaType BOOLEAN = new Simple("boolean", Type.BOOLEAN);
	static final LuaType NUMBER = new Simple("number", Type.DOUBLE);
	static final LuaType STRING = new Simple("string", Type.STRING);
	// TODO userdata, thread, table
	// TODO table specialization
	
	// code4jvm-specific types
	static final LuaType UNKNOWN = new Simple("unknown", Type.OBJECT);
	
	public static LuaType of(Type type) {
		return LuaTypeSupport.TYPE_TO_TYPE.getOrDefault(type, LuaType.UNKNOWN);
	}
	
	public static LuaType of(Value value) {
		return of(value.type());
	}
	
	public static LuaType of(Class<?> type) {
		return LuaTypeSupport.CLASS_TO_TYPE.getOrDefault(type, LuaType.UNKNOWN);
	}
	
	public static Tuple tuple(LuaType... types) {
		return new Tuple(types);
	}
	
	public static Function function(UpvalueTemplate[] upvalues, LuaLocalVar[] args, LuaBlock body) {
		return new Function(upvalues, args, body);
	}
	
	public static List<LuaType> readList(String str) {
		var types = new ArrayList<LuaType>();
		for (var i = 0; i < str.length(); i++) {
			types.add(switch (str.charAt(i)) {
			case 'V' -> LuaType.NIL;
			case 'B' -> LuaType.BOOLEAN;
			case 'N' -> LuaType.NUMBER;
			case 'S' -> LuaType.STRING;
			case 'U' -> LuaType.UNKNOWN;
			case 'T' -> throw new UnsupportedOperationException("todo");
			case 'F' -> throw new UnsupportedOperationException("todo");
			default -> throw new IllegalArgumentException("unknown type: " + str.charAt(i));
			});
		}
		return types;
	}
	
	String name();
	
	Type backingType();
}
