package fi.benjami.code4jvm.lua;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Scanner;

import fi.benjami.code4jvm.lua.ir.LuaType;

public class LuaStdLib {
	
	private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
	
	public static final MethodHandle PRINT, TO_STRING, TYPE, TO_NUMBER, READ, WRITE;
	
	static {
		try {
			PRINT = LOOKUP.findStatic(LuaStdLib.class, "print", MethodType.methodType(void.class, Object.class));
			TO_STRING = LOOKUP.findVirtual(Object.class, "toString", MethodType.methodType(String.class));
			TYPE = LOOKUP.findStatic(LuaStdLib.class, "type", MethodType.methodType(String.class, Object.class));
			TO_NUMBER = LOOKUP.findStatic(Double.class, "parseDouble", MethodType.methodType(double.class, String.class));
			READ = LOOKUP.findStatic(LuaStdLib.class, "read", MethodType.methodType(String.class));
			WRITE = LOOKUP.findStatic(LuaStdLib.class, "write", MethodType.methodType(void.class, String.class));
		} catch (NoSuchMethodException | IllegalAccessException e) {
			throw new AssertionError(e);
		}
	}

	public static void print(Object obj) {
		System.out.println(obj);
	}
	
	public static String type(Object obj) {
		return LuaType.of(obj).name();
	}
	
	private static final Scanner SCANNER = new Scanner(System.in);
	
	public static String read() {
		return SCANNER.nextLine();
	}
	
	public static void write(String str) {
		System.out.print(str);
	}

}
