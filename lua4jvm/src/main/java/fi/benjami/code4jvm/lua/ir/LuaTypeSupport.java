package fi.benjami.code4jvm.lua.ir;

import java.util.Map;

import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.lua.runtime.LuaTable;

class LuaTypeSupport {

	public static final Map<Type, LuaType> TYPE_TO_TYPE = Map.of(
			Type.BOOLEAN, LuaType.BOOLEAN,
			Type.of(Boolean.class), LuaType.BOOLEAN,
			Type.DOUBLE, LuaType.NUMBER,
			Type.of(Double.class), LuaType.NUMBER,
			Type.STRING, LuaType.STRING,
			LuaTable.TYPE, LuaType.TABLE
	);
	
	public static final Map<Class<?>, LuaType> CLASS_TO_TYPE = Map.of(
			boolean.class, LuaType.BOOLEAN,
			Boolean.class, LuaType.BOOLEAN,
			double.class, LuaType.NUMBER,
			Double.class, LuaType.NUMBER,
			String.class, LuaType.STRING,
			LuaTable.class, LuaType.TABLE
	);
}
