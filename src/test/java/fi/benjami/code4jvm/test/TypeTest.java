package fi.benjami.code4jvm.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import fi.benjami.code4jvm.Type;

@ExtendWith({EnableDebugExtension.class})
public class TypeTest {

	private record Row(
			Type staticField,
			Type ofClass,
			Type ofName
			) {}
	
	private static final Row[] TYPES = new Row[] {
			new Row(Type.BOOLEAN, Type.of(boolean.class), Type.of("boolean", false)),
			new Row(Type.BYTE, Type.of(byte.class), Type.of("byte", false)),
			new Row(Type.SHORT, Type.of(short.class), Type.of("short", false)),
			new Row(Type.CHAR, Type.of(char.class), Type.of("char", false)),
			new Row(Type.INT, Type.of(int.class), Type.of("int", false)),
			new Row(Type.LONG, Type.of(long.class), Type.of("long", false)),
			new Row(Type.FLOAT, Type.of(float.class), Type.of("float", false)),
			new Row(Type.DOUBLE, Type.of(double.class), Type.of("double", false)),
			new Row(Type.OBJECT, Type.of(Object.class), Type.of("java.lang.Object", false))
	};
	
	@Test
	public void createMethods() {
		for (var row : TYPES) {
			assertEquals(row.staticField, row.ofClass);
			assertEquals(row.staticField, row.ofName);
		}
	}
	
	// TODO better test coverage

}
