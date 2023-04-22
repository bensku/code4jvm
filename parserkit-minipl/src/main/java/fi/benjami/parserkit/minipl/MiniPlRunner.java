package fi.benjami.parserkit.minipl;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import fi.benjami.parserkit.lexer.Lexer;
import fi.benjami.parserkit.lexer.TokenTransformer;
import fi.benjami.parserkit.lexer.TokenizedText;
import fi.benjami.parserkit.minipl.compiler.MiniPlCompiler;
import fi.benjami.parserkit.minipl.compiler.MiniPlTypeChecker;
import fi.benjami.parserkit.minipl.compiler.SemanticError;
import fi.benjami.parserkit.minipl.compiler.TypeTable;
import fi.benjami.parserkit.minipl.parser.HandWrittenLexer;
import fi.benjami.parserkit.minipl.parser.MiniPlError;
import fi.benjami.parserkit.minipl.parser.MiniPlNodes;
import fi.benjami.parserkit.minipl.parser.MiniPlTokenType;
import fi.benjami.parserkit.minipl.parser.MiniPlTransformer;
import fi.benjami.parserkit.parser.ParseError;
import fi.benjami.parserkit.parser.Parser;

public class MiniPlRunner {
	
	private final Lexer lexer;
	private final TokenTransformer transformer;
	private final Parser parser;
	
	private final List<WrappedError> errors;
	
	public MiniPlRunner() {
		this.lexer = new HandWrittenLexer();
		this.transformer = new MiniPlTransformer();
		this.parser = Parser.compileAndLoad(MiniPlNodes.REGISTRY, MiniPlTokenType.values());
		this.errors = new ArrayList<>();
	}

	public MiniPlNodes.Program parse(String src) {
		// FIXME why are tabs not working?
		src = src.replace("\t", " ");
		
		var tokens = new TokenizedText(lexer, transformer);
		var view = tokens.apply(src, 0, 0);
		var result = parser.parseFully(MiniPlNodes.Program.class, view);
		for (var error : result.errors()) {
			errors.add(new WrappedError(error, error.start()));
		}
		// TODO error handling
		return result.node();
	}
	
	public Optional<TypeTable> typeCheck(MiniPlNodes.Program program) {
		if (program == null) {
			return Optional.empty(); // Catastrophic parse failure
		}
		
		var typeChecker = new MiniPlTypeChecker();
		program.visit(typeChecker);
		for (var error : typeChecker.errors()) {
			errors.add(new WrappedError(error, Integer.MAX_VALUE));
		}
		return typeChecker.errors().isEmpty() ? Optional.of(typeChecker.toTypeTable()) : Optional.empty();
	}
	
	public byte[] compile(MiniPlNodes.Program program, TypeTable types) {
		if (!errors.isEmpty()) {
			throw new IllegalStateException("cannot compile with errors");
		}
		return new MiniPlCompiler(types, program).compile();
	}
	
	public List<WrappedError> errors() {
		return errors.stream().sorted().toList();
	}
	
	public String printErrors() {
		return errors.stream()
				.sorted()
				.map(this::printError)
				.collect(Collectors.joining("\n"));
	}
	
	private String printError(WrappedError error) {
		if (error.error() instanceof ParseError parseError) {
			var typeStr = switch (parseError.type()) {
			case ParseError.LEXICAL -> "unknown token";
			case ParseError.NOT_FULLY_PARSED -> "unknown parse error";
			case MiniPlError.MISSING_EXPRESSION -> "expected an expression";
			case MiniPlError.MISSING_SEMICOLON -> "expected a semicolon";
			default -> "unknown error";
			};
			return "parse error: " + typeStr + " at index " + parseError.start();
		} else if (error.error() instanceof SemanticError semanticError) {
			return "semantic error: " + semanticError.type().errorMsg(semanticError.fmtArgs());
		} else {
			throw new AssertionError();
		}
	}
	
	public Runnable load(byte[] code) {
		try {
			return (Runnable) MethodHandles.lookup().defineHiddenClass(code, true).lookupClass()
					.getConstructor()
					.newInstance();
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			throw new AssertionError("failed to load code", e);
		}
	}
}
