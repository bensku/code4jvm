package fi.benjami.code4jvm.lua;

import java.nio.file.Files;
import java.nio.file.Path;

import fi.benjami.code4jvm.lua.parser.LuaLexer;
import fi.benjami.code4jvm.lua.parser.LuaNode;
import fi.benjami.code4jvm.lua.parser.LuaToken;
import fi.benjami.code4jvm.lua.parser.LuaTokenTransformer;
import fi.benjami.code4jvm.lua.runtime.LuaTable;
import fi.benjami.parserkit.parser.Parser;

public class LuaRunner {

	public static void main(String... args) throws Throwable {
		var script = Files.readString(Path.of(args[0]));
		
		var parser  = Parser.compileAndLoad(LuaNode.REGISTRY, LuaToken.values());
		var vm = new LuaVm(new LuaLexer(), new LuaTokenTransformer(), parser);
		
		// Make rest of arguments available to script
		var argTable = new LuaTable(args.length - 1);
		for (var i = 1; i < args.length; i++) {
			argTable.set((double) i, args[i]);
		}
		vm.globals().set("arg", argTable);
		
		vm.execute(script);
	}
}
