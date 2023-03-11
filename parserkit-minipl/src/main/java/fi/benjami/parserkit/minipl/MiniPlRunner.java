package fi.benjami.parserkit.minipl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class MiniPlRunner {

	public static void main(String... args) throws IOException {
		var file = Path.of(args[0]);
		var src = Files.readString(file);
		
		// TODO
	}
}
