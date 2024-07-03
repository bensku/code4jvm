package fi.benjami.code4jvm.lua.stdlib;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import fi.benjami.code4jvm.lua.LuaVm;
import fi.benjami.code4jvm.lua.ffi.LuaLibrary;
import fi.benjami.code4jvm.lua.ffi.Nullable;
import fi.benjami.code4jvm.lua.ir.LuaType;
import fi.benjami.code4jvm.lua.ffi.Inject;
import fi.benjami.code4jvm.lua.ffi.JavaFunction;
import fi.benjami.code4jvm.lua.ffi.LuaBinder;
import fi.benjami.code4jvm.lua.ffi.LuaExport;
import fi.benjami.code4jvm.lua.runtime.LuaTable;

public class BasicLib implements LuaLibrary {
	
	public static final BasicLib INSTANCE = new BasicLib();
		
	private static final Collection<JavaFunction> FUNCTIONS = new LuaBinder(MethodHandles.lookup()).bindFunctionsFrom(BasicLib.class);
	
	private BasicLib() {}
	
	@Override
	public void install(LuaVm vm) {
		var globals = vm.globals();
		for (var func : FUNCTIONS) {
			globals.set(func.name(), func);
		}
		globals.set("_G", globals);
		globals.set("_VERSION", "lua4jvm 0.1 (Lua 5.4)"); // TODO derive from build config
	}

	// TODO assert
	
	@LuaExport("collectgarbage")
	private static void collectgarbage(String opt) {
		if ("collect".equals(opt)) {
			System.gc();
		} // else: silently ignore, Lua code can't observe this (except for performance)
	}
	
	// TODO move dofile out of here to (e.g.) BasicIoLib, because it can read files from disk!
	
	@LuaExport("dofile")
	private static Object dofile(@Inject LuaVm vm, String filename) {
		String text;
		try {
			text = Files.readString(Path.of(filename));
		} catch (IOException e) {
			throw new LuaException("failed to load " + filename, e);
		}
		try {
			return vm.execute(text);
		} catch (Throwable e) {
			throw new LuaException("failed to execute " + filename, e);
		}
	}
	
	@LuaExport("error")
	private static void error(Object message) {
		// TODO use level? currently not possible, and potentially redundant when stack traces are improved
		throw new LuaException(message);
	}
	
	@LuaExport("getmetatable")
	private static LuaTable getmetatable(Object object) {
		if (object instanceof LuaTable table) {
			return table.metatable();
		}
		return null;
	}
	
	@LuaExport("setmetatable")
	private static LuaTable setmetatable(LuaTable table, @Nullable LuaTable metatable) {
		table.metatable(metatable);
		return table;
	}
	
	// TODO iteration: ipairs, pairs
	
	// TODO load: it is actually quite complex
	// TODO loadfile
	
	// TODO pcall - but should this be a normal function?
	
	@LuaExport("print")
	private static void print(@Inject LuaVm vm, Object... args) {
		var text = Arrays.stream(args)
				.map(Object::toString)
				.collect(Collectors.joining("\t"));
		vm.options().stdOut().println(text);
	}
	
	@LuaExport("tonumber")
	public static Object tonumber(Object value) {
		if (value instanceof Double) {
			return value;
		} else if (value instanceof String str) {
			try {				
				return Double.parseDouble(str);
			} catch (NumberFormatException e) {
				return null;
			}
		} else {
			return null;
		}
	}
	
	@LuaExport("tostring")
	private static String tostring(Object value) {
		return value.toString();
	}
	
	@LuaExport("type")
	public static String type(Object value) {
		return LuaType.of(value).name();
	}
	
}
