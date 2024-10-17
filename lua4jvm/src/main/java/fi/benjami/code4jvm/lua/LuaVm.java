package fi.benjami.code4jvm.lua;

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.misc.ParseCancellationException;

import fi.benjami.code4jvm.lua.compiler.IrCompiler;
import fi.benjami.code4jvm.lua.compiler.LuaScope;
import fi.benjami.code4jvm.lua.compiler.LuaSyntaxException;
import fi.benjami.code4jvm.lua.ir.LuaLocalVar;
import fi.benjami.code4jvm.lua.ir.LuaModule;
import fi.benjami.code4jvm.lua.ir.LuaType;
import fi.benjami.code4jvm.lua.ir.UpvalueTemplate;
import fi.benjami.code4jvm.lua.parser.LuaLexer;
import fi.benjami.code4jvm.lua.parser.LuaParser;
import fi.benjami.code4jvm.lua.parser.LuaParser.ChunkContext;
import fi.benjami.code4jvm.lua.runtime.LuaFunction;
import fi.benjami.code4jvm.lua.runtime.LuaTable;

public class LuaVm {
	
	private final VmOptions options;
	private final LuaTable globals;
	
	public LuaVm() {
		this(VmOptions.DEFAULT);
	}
	
	public LuaVm(VmOptions options) {
		this.options = options;
		this.globals = new LuaTable();
		installLibraries();
	}
	
	public VmOptions options() {
		return options;
	}
	
	private void installLibraries() {
		for (var lib : options.libraries()) {
			lib.install(this);
		}
	}
	
	public LuaTable globals() {
		return globals;
	}
	
	public LuaModule compile(String name, String chunk) {
		// Tokenize and parse the chunk
		var lexer = new LuaLexer(CharStreams.fromString(chunk));
		var parser = new LuaParser(new CommonTokenStream(lexer));
		parser.setErrorHandler(new BailErrorStrategy());
		
		ChunkContext tree;
		try {			
			tree = parser.chunk();
		} catch (ParseCancellationException e) {
			// Parse error; try again, this time with error recovery and no IR generation
			lexer = new LuaLexer(CharStreams.fromString(chunk));
			parser = new LuaParser(new CommonTokenStream(lexer));
			parser.removeErrorListeners();
			
			var errors = new ArrayList<LuaSyntaxException.Error>();
			parser.addErrorListener(new BaseErrorListener() {
				@Override
				public void syntaxError(Recognizer<?, ?> recognizer,
						Object offendingSymbol,
						int line,
						int charPositionInLine,
						String msg,
						RecognitionException e) {
					errors.add(new LuaSyntaxException.Error(name, line, charPositionInLine, msg));
				}
			});
			tree = parser.chunk();
			throw new LuaSyntaxException(errors);
		}
		
		// Perform semantic analysis and compile to IR
		var rootScope = LuaScope.chunkRoot();
		var visitor = new IrCompiler(name, rootScope);
		return new LuaModule(name, visitor.visitChunk(tree), (LuaLocalVar) rootScope.resolve("_ENV"));
	}
	
	public LuaModule compile(String chunk) {
		return compile("unknown", chunk);
	}
	
	public LuaFunction load(LuaModule module, LuaTable env) {
		// Instantiate the module
		module.env().markMutable(); // Initial assignment by VM
		var type = LuaType.function(
				// TODO _ENV mutability tracking
				List.of(new UpvalueTemplate(module.env(), module.env().mutable() ? LuaType.UNKNOWN : LuaType.TABLE, module.env().mutable())),
				List.of(),
				module.root(),
				module.name(),
				"main chunk"
				);
		return new LuaFunction(this, type, new Object[] {env});
	}
	
	public Object execute(String name, String chunk) throws Throwable {
		var module = compile(name, chunk);
		var func = load(module, globals());
		return func.call();
	}
	
	public Object execute(String chunk) throws Throwable {
		return execute("unknown", chunk);
	}
}
