package fi.benjami.code4jvm.lua.compiler;

class SingleClassLoader extends ClassLoader {
	
	public static Class<?> load(String name, byte[] code) {
		return new SingleClassLoader(code).findClass(name);
	}
	
	private final byte[] code;
	
	public SingleClassLoader(byte[] code) {
		this.code = code;
	}

	@Override
	public Class<?> findClass(String name) {
		return defineClass(name, code, 0, code.length);
	}
}
