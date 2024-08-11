package fi.benjami.code4jvm.lua.ir;

import java.util.Map;

import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.lua.runtime.LuaTable;

class LuaTypeSupport {

	public static final Map<Type, LuaType> TYPE_TO_TYPE = Map.of(
			Type.BOOLEAN, LuaType.BOOLEAN,
			Type.of(Boolean.class), LuaType.BOOLEAN,
			Type.INT, LuaType.INTEGER,
			Type.of(Integer.class), LuaType.INTEGER,
			Type.DOUBLE, LuaType.FLOAT,
			Type.of(Double.class), LuaType.FLOAT,
			Type.STRING, LuaType.STRING,
			LuaTable.TYPE, LuaType.TABLE
	);
	
	public static final Map<Class<?>, LuaType> CLASS_TO_TYPE = Map.of(
			boolean.class, LuaType.BOOLEAN,
			Boolean.class, LuaType.BOOLEAN,
			int.class, LuaType.INTEGER,
			Integer.class, LuaType.INTEGER,
			double.class, LuaType.FLOAT,
			Double.class, LuaType.FLOAT,
			String.class, LuaType.STRING,
			LuaTable.class, LuaType.TABLE
	);
}
