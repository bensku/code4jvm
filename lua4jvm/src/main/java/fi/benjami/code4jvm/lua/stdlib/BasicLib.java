package fi.benjami.code4jvm.lua.stdlib;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import fi.benjami.code4jvm.lua.LuaVm;
import fi.benjami.code4jvm.lua.ffi.LuaLibrary;
import fi.benjami.code4jvm.lua.ffi.Nullable;
import fi.benjami.code4jvm.lua.ir.LuaType;
import fi.benjami.code4jvm.lua.linker.CallSiteOptions;
import fi.benjami.code4jvm.lua.linker.LuaCallSite;
import fi.benjami.code4jvm.lua.linker.LuaLinker;
import fi.benjami.code4jvm.lua.ffi.Inject;
import fi.benjami.code4jvm.lua.ffi.JavaFunction;
import fi.benjami.code4jvm.lua.ffi.LuaBinder;
import fi.benjami.code4jvm.lua.ffi.LuaExport;
import fi.benjami.code4jvm.lua.ffi.LuaIntrinsic;
import fi.benjami.code4jvm.lua.runtime.LuaFunction;
import fi.benjami.code4jvm.lua.runtime.LuaTable;

public class BasicLib implements LuaLibrary {
	
	public static final BasicLib INSTANCE = new BasicLib();
	
	private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
	private static final Collection<JavaFunction> FUNCTIONS = new LuaBinder(LOOKUP).bindFunctionsFrom(BasicLib.class);
	
	private BasicLib() {}
	
	@Override
	public void install(LuaVm vm) {
		var globals = vm.globals();
		for (var func : FUNCTIONS) {
			globals.set(func.name(), func);
		}
		globals.set("error", makeError());
		globals.set("_G", globals);
		globals.set("_VERSION", "lua4jvm 0.1 (Lua 5.4)"); // TODO derive from build config
	}
	
	// Do this the hard way to avoid stack frames from error function itself
	private static JavaFunction makeError() {
		try {
			var newError = LOOKUP.findConstructor(LuaException.class, MethodType.methodType(void.class, Object.class));
			var throwError = MethodHandles.dropArguments(MethodHandles.throwException(void.class, LuaException.class), 1, Object.class);
			var target = MethodHandles.foldArguments(throwError, newError);
			return new JavaFunction("error", List.of(new JavaFunction.Target(
					List.of(),
					List.of(new JavaFunction.Arg("message", LuaType.UNKNOWN, false)),
					false,
					LuaType.NIL,
					false,
					target,
					null
			)), null);
		} catch (NoSuchMethodException | IllegalAccessException e) {
			throw new AssertionError();
		}
	}

	// TODO assert
	
	@LuaExport("collectgarbage")
	private static void collectgarbage(String opt) {
		if ("collect".equals(opt)) {
			System.gc();
		} // else: silently ignore, Lua code can't observe this (except for performance)
	}
	
	@LuaExport("load")
	public static LuaFunction load(@Inject LuaVm vm, Object chunk, String chunkname, String mode, LuaTable env) {
		if (mode.equals("b")) {
			throw new UnsupportedOperationException("lua4jvm does not support binary chunks");
		}
		String code;
		if (chunk instanceof String text) {
			code = text;
		} else {
			throw new UnsupportedOperationException("callable chunks are not yet supported");
		}
		
		var module = vm.compile(chunkname, code);
		return vm.load(module, env);
	}
	
	@LuaExport("load")
	public static LuaFunction load(@Inject LuaVm vm, Object chunk, String chunkname, String mode) {
		// According to Lua 5.4 spec, load defaults to VM's globals, not whatever current _ENV might be!
		return load(vm, chunk, chunkname, mode, vm.globals());
	}
	
	@LuaExport("load")
	public static LuaFunction load(@Inject LuaVm vm, Object chunk, String chunkname) {
		return load(vm, chunk, chunkname, "t");
	}
	
	@LuaExport("load")
	public static LuaFunction load(@Inject LuaVm vm, Object chunk) {
		return load(vm, chunk, chunk instanceof String ? "chunk" : "=(load)");
	}
	
	@LuaExport("loadfile")
	public static LuaFunction loadfile(@Inject LuaVm vm, String filename, String mode, LuaTable env) {
		String text;
		if (filename == null) {
			var stdin = vm.options().stdIn().orElseThrow(() -> new LuaException("stdin is not available"));
			try {
				text = new String(stdin.readAllBytes());
			} catch (IOException e) {
				throw new LuaException("failed to read VM stdin", e);
			}
			filename = "stdin";
		} else {			
			var fs = vm.options().fileSystem().orElseThrow(() -> new LuaException("file system not available"));
			var path = fs.getPath(filename);
			
			try {
				text = Files.readString(path);
			} catch (IOException e) {
				throw new LuaException("failed to read " + filename, e);
			}
		}
		return load(vm, text, filename, mode, env);
	}
	
	@LuaExport("loadfile")
	public static LuaFunction loadfile(@Inject LuaVm vm, String filename, String mode) {
		return loadfile(vm, filename, mode, vm.globals());
	}
	
	@LuaExport("loadfile")
	public static LuaFunction loadfile(@Inject LuaVm vm, String filename) {
		return loadfile(vm, filename, "t");
	}
	
	@LuaExport("loadfile")
	public static LuaFunction loadfile(@Inject LuaVm vm) {
		return loadfile(vm, null);
	}
	
	@LuaExport("dofile")
	private static Object dofile(@Inject LuaVm vm, String filename) {
		var func = loadfile(vm, filename);
		try {
			return func.call();
		} catch (Throwable e) {
			throw new LuaException("failed to execute " + filename, e);
		}
	}
	
	@LuaExport("dofile")
	private static Object dofile(@Inject LuaVm vm) {
		return dofile(vm, null);
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
		
	// TODO pcall - but should this be a normal function?
	
	@LuaExport("print")
	private static void print(@Inject LuaVm vm, Object... args) {
		var stdout = vm.options().stdOut().orElseThrow(() -> new LuaException("stdout is not available"));
		var text = Arrays.stream(args)
				.map(Objects::toString)
				.collect(Collectors.joining("\t"));
		stdout.println(text);
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
	
	private static final JavaFunction INTRINSIC_ITERATOR = InternalLib.FUNCTIONS.get("intrinsicIterator"),
			TABLE_ITERATOR = InternalLib.FUNCTIONS.get("tableIterator"),
			ARRAY_ITERATOR = InternalLib.FUNCTIONS.get("arrayIterator");
		
	@LuaExport("pairs")
	@LuaIntrinsic("iteratorFor")
	private static Object[] pairsStateful(@Inject LuaVm vm, Object iterable) throws Throwable {
		if (iterable instanceof LuaTable table
				&& (table.metatable() == null || table.metatable().get("__pairs") == null)) {
			// Normal Lua table; let's cheat a bit and use a stateful table iterator (intrinsic path)
			return new Object[] {INTRINSIC_ITERATOR, table.iterator()};
		}
		
		// Aside of the above fast path, delegate to normal pairs
		return pairs(vm, iterable);
	}
	
	@LuaExport("pairs")
	private static Object[] pairs(@Inject LuaVm vm, Object iterable) throws Throwable {
		if (iterable instanceof LuaTable table) {
			if (table.metatable() != null) {
				var metamethod = table.metatable().get("__pairs");
				if (metamethod != null) {
					// Call __pairs and use whatever it returns as an iterator
					var target = LuaLinker.linkCall(new LuaCallSite(null, CallSiteOptions.nonFunction(vm, LuaType.TABLE)),
							metamethod, table);
					return (Object[]) target.target().invoke(metamethod, table);
				}
			}
			// No __pairs, just iterate over the table normally (non-intrinsic path)
			return new Object[] {TABLE_ITERATOR, table};
		} else {
			throw new LuaException("value not iterable");
		}
	}
	
	@LuaExport("ipairs")
	@LuaIntrinsic("iteratorFor")
	private static Object[] ipairsStateful(@Inject LuaVm vm, Object iterable) throws Throwable {
		if (iterable instanceof LuaTable table
				&& (table.metatable() == null || table.metatable().get("__ipairs") == null)) {
			// Normal Lua table; let's cheat a bit and use a stateful table iterator (intrinsic path)
			return new Object[] {INTRINSIC_ITERATOR, table.arrayIterator()};
		}
		
		// Aside of the above fast path, delegate to normal pairs
		return pairs(vm, iterable);
	}
	
	@LuaExport("ipairs")
	private static Object[] ipairs(@Inject LuaVm vm, Object iterable) throws Throwable {
		if (iterable instanceof LuaTable table) {
			if (table.metatable() != null) {
				var metamethod = table.metatable().get("__ipairs");
				if (metamethod != null) {
					// Call __pairs and use whatever it returns as an iterator
					var target = LuaLinker.linkCall(new LuaCallSite(null, CallSiteOptions.nonFunction(vm, LuaType.TABLE)),
							metamethod, table);
					return (Object[]) target.target().invoke(metamethod, table);
				}
			}
			// No __pairs, just iterate over the table normally (non-intrinsic path)
			return new Object[] {ARRAY_ITERATOR, table};
		} else {
			throw new LuaException("value not iterable");
		}
	}
	
	@LuaExport("next")
	public static Object[] next(LuaTable table, Object prevKey) {
		return table.next(prevKey);
	}
	
	@LuaExport("next")
	public static Object[] next(LuaTable table) {
		return table.next(null);
	}
	
}
