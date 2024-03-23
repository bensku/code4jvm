package fi.benjami.code4jvm.lua;

import java.util.List;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import fi.benjami.code4jvm.lua.compiler.IrCompiler;
import fi.benjami.code4jvm.lua.ir.LuaLocalVar;
import fi.benjami.code4jvm.lua.ir.LuaModule;
import fi.benjami.code4jvm.lua.ir.LuaType;
import fi.benjami.code4jvm.lua.ir.UpvalueTemplate;
import fi.benjami.code4jvm.lua.parser.LuaLexer;
import fi.benjami.code4jvm.lua.parser.LuaParser;
import fi.benjami.code4jvm.lua.runtime.LuaFunction;
import fi.benjami.code4jvm.lua.runtime.LuaTable;
import fi.benjami.code4jvm.lua.semantic.LuaScope;

public class LuaVm {
	
	private final LuaTable globals;
	
	public LuaVm() {
		this.globals = initGlobals();
	}
	
	private static LuaTable initGlobals() {
		var globals = new LuaTable();
		
		globals.set("_G", globals);
		globals.set("_VERSION", "lua4jvm DEV (Lua 5.4)");
		
		globals.set("print", LuaStdLib.PRINT);
		globals.set("tostring", LuaStdLib.TO_STRING);
		globals.set("type", LuaStdLib.TYPE);
		globals.set("tonumber", LuaStdLib.TO_NUMBER);
		
		var ourLib = new LuaTable();
		ourLib.set("read", LuaStdLib.READ);
		ourLib.set("write", LuaStdLib.WRITE);
		globals.set("code4jvm", ourLib);
		
		return globals;
	}
	
	public LuaTable globals() {
		return globals;
	}
	
	public LuaModule compile(String chunk) {
		// Tokenize and parse the chunk
		var lexer = new LuaLexer(CharStreams.fromString(chunk));
		var parser = new LuaParser(new CommonTokenStream(lexer));
		var tree = parser.chunk();
		
		// Perform semantic analysis and compile to IR
		var rootScope = LuaScope.chunkRoot();
		var visitor = new IrCompiler(rootScope);
		return new LuaModule(visitor.visitChunk(tree), (LuaLocalVar) rootScope.resolve("_ENV"));
	}
	
	public LuaFunction load(LuaModule module, LuaTable env) {
		// Instantiate the module
		var type = LuaType.function(
				List.of(new UpvalueTemplate(module.env(), LuaType.TABLE)),
				List.of(),
				module.root()
				);
		return new LuaFunction(type, new Object[] {env});
	}
	
	public Object execute(String chunk) throws Throwable {
		var module = compile(chunk);
		var func = load(module, globals());
		return func.call();
	}
}
