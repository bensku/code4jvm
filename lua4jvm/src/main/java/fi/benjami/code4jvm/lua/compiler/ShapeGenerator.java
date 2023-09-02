package fi.benjami.code4jvm.lua.compiler;

import java.lang.invoke.MethodHandles;

import fi.benjami.code4jvm.Condition;
import fi.benjami.code4jvm.Constant;
import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.flag.Access;
import fi.benjami.code4jvm.lua.ir.LuaType;
import fi.benjami.code4jvm.lua.runtime.LuaTable;
import fi.benjami.code4jvm.statement.Return;
import fi.benjami.code4jvm.structure.IfBlock;
import fi.benjami.code4jvm.typedef.ClassDef;

public class ShapeGenerator {
	
	private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
	
	public static Class<?> newShape(LuaType.Shape type) {
		var code = generateCode(type);
		
		// FIXME this probably leaks memory
		try {
			return LOOKUP.defineClass(code);
		} catch (IllegalAccessException e) {
			throw new AssertionError(e);
		}
		
		// NOTE: this won't work, because hidden classes need to refer to shapes
//		var loader = new SingleClassLoader(code);
//		try {
//			return loader.loadClass("fi.benjami.code4jvm.lua.compiler.Shape");
//		} catch (ClassNotFoundException e) {
//			throw new AssertionError(e);
//		}
	}
	
	private static byte[] generateCode(LuaType.Shape shape) {
		var entries = shape.knownEntries();
		
		var def = ClassDef.create("fi.benjami.code4jvm.lua.compiler.Shape" + System.identityHashCode(shape), Access.PUBLIC);
		def.superClass(LuaTable.TYPE);
		
		// TODO specialized (=number) array path? or do it runtime?
		
		// Add constructor
		var constructor = def.addConstructor(Access.PUBLIC);
		constructor.add(constructor.self().callPrivate(LuaTable.TYPE, Type.VOID, "<init>"));
		if (shape.shouldInitializeMap()) {
			// TODO attempt to guess map size (how?)
			constructor.add(constructor.self().callVirtual(Type.VOID, "initializeMap", Constant.of(8)));
		}
		constructor.add(Return.nothing());
		
		// Override LuaTable getter and setter
		var getter = def.addMethod(Type.OBJECT, "get", Access.PUBLIC);
		var getterName = getter.arg(Type.OBJECT);
		var getterClauses = new IfBlock();
		
		var setter = def.addMethod(Type.VOID, "set", Access.PUBLIC);
		var setterName = setter.arg(Type.OBJECT);
		var setterValue = setter.arg(Type.OBJECT);
		var setterClauses = new IfBlock();
		
		// Store everything as j.l.Object; box numbers as needed
		// This is not very efficient, but Lua tables are mutable unlike functions!
		for (var entry : entries.entrySet()) {
			var fieldName = "_" + entry.getKey();
			def.addInstanceField(Access.PUBLIC, Type.OBJECT, fieldName);
			
			// Append to generic accessor lookup table
			// TODO what if we have MANY entries?
			Constant key = Constant.of(entry.getKey());
			getterClauses.branch(Condition.equal(key.cast(Type.OBJECT), getterName), block -> {
				var fieldValue = block.add(getter.self().getField(Type.OBJECT, fieldName));
				block.add(Return.value(fieldValue));
			});
			setterClauses.branch(Condition.equal(key.cast(Type.OBJECT), getterName), block -> {
				block.add(setter.self().putField(fieldName, setterValue));
			});
		}
		
		// Fallback to map
		getter.add(getterClauses);
		if (!shape.shouldInitializeMap()) {			
			// If map is not guaranteed to be initialized, check and initialize if needed
			getter.add(getter.self().callVirtual(Type.VOID, "initializeMap", Constant.of(8)));
		}
		var mapValue = getter.add(getter.self().callPrivate(LuaTable.TYPE, Type.OBJECT, "get", getterName));
		getter.add(Return.value(mapValue));
		
		setterClauses.fallback(block -> {
			if (!shape.shouldInitializeMap()) {
				// If map is not guaranteed to be initialized, check and initialize if needed
				block.add(setter.self().callVirtual(Type.VOID, "initializeMap", Constant.of(8)));
			}
			block.add(setter.self().callPrivate(LuaTable.TYPE, Type.VOID, "set", setterName, setterValue));
		});
		setter.add(setterClauses);
		setter.add(Return.nothing());
		
		// TODO size(), but how to implement it efficiently AND accurately?
		
		return def.compile();
	}

}
