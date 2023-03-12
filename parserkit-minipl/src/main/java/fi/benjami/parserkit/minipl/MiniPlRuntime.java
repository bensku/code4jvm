package fi.benjami.parserkit.minipl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import fi.benjami.code4jvm.Type;

public class MiniPlRuntime {
	
	public static final Type TYPE = Type.of(MiniPlRuntime.class);
	
	private static final BufferedReader READER = new BufferedReader(new InputStreamReader(System.in));

	public static String readString() throws IOException {
		return READER.readLine();
	}
	
	public static int readInt() throws IOException {
		var str = readString();
		try {
			return Integer.parseInt(str);
		} catch (NumberFormatException e) {
			throw new MiniPlException("invalid int read from stdin", e);
		}
	}
	
	public static void print(int i) {
		System.out.print(i);
	}
	
	public static void print(String s) {
		System.out.print(s);
	}
}
