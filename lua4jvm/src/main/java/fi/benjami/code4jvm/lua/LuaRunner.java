package fi.benjami.code4jvm.lua;

import java.nio.file.Files;
import java.nio.file.Path;

import fi.benjami.code4jvm.lua.runtime.LuaTable;

public class LuaRunner {

	public static void main(String... args) throws Throwable {
		var script = Files.readString(Path.of(args[0]));
		
		var vm = new LuaVm();
		
		// Make rest of arguments available to script
		var argTable = LuaTable.newTable(args.length - 1);
		for (var i = 1; i < args.length; i++) {
			argTable.set((double) i, args[i]);
		}
		vm.globals().set("arg", argTable);
		
		vm.execute(script);
	}
}
