package fi.benjami.code4jvm.lua.compiler;

public class SingleClassLoader extends ClassLoader {
	
	private final byte[] code;
	
	public SingleClassLoader(byte[] code) {
		this.code = code;
	}

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		return defineClass(name, code, 0, code.length);

	}
}
