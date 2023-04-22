package fi.benjami.parserkit.minipl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import fi.benjami.parserkit.minipl.compiler.TypeTable;

public class AppMain {

	public static void main(String... args) throws IOException {
		var file = Path.of(args[0]);
		var src = Files.readString(file);
		
		var runner = new MiniPlRunner();
		var prog = runner.parse(src);
		Optional<TypeTable> types;
		try {			
			types = runner.typeCheck(prog);
		} catch (Exception e) {
			System.err.println(runner.printErrors());
			System.err.println("Semantic analysis failed:");
			e.printStackTrace();
			System.exit(3);
			return;
		}
		
		// If there are compilation errors, report them and exit
		if (!runner.errors().isEmpty()) {
			System.err.println(runner.printErrors());
			System.exit(1);
		}
		
		// Otherwise, compile, load and execute the program
		var code = runner.compile(prog, types.orElseThrow(() -> new AssertionError("critical parse failure")));
		var app = runner.load(code);
		try {
			app.run();			
		} catch (MiniPlException e) {
			System.err.println("Runtime error: " + e.getMessage());
			System.exit(2);
		}
		// Normal exit
	}
}
