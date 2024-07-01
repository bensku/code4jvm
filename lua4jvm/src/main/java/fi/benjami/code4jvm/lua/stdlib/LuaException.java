package fi.benjami.code4jvm.lua.stdlib;

public class LuaException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final Object message;
	
	public LuaException(Object message) {
		super(message.toString());
		this.message = message;
	}
	
	public LuaException(Object message, Throwable cause) {
		super(message.toString(), cause);
		this.message = message;
	}
	
	public Object getLuaMessage() {
		return message;
	}
}
