package fi.benjami.code4jvm.lua.ir;

import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.Value;
import fi.benjami.code4jvm.lua.runtime.LuaFunction;

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
			return Type.OBJECT.array(types.length);
		}
	}
	
	// Lua standard types
	static final LuaType NIL = new Simple("nil", Type.OBJECT);
	static final LuaType BOOLEAN = new Simple("boolean", Type.BOOLEAN);
	static final LuaType NUMBER = new Simple("number", Type.DOUBLE);
	static final LuaType STRING = new Simple("string", Type.STRING);
	static final LuaType FUNCTION = new Simple("function", Type.of(LuaFunction.class));
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
	
	String name();
	
	Type backingType();
}
