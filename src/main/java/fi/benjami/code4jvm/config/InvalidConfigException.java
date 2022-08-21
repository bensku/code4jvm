package fi.benjami.code4jvm.config;

public class InvalidConfigException extends RuntimeException {

	private static final long serialVersionUID = 1L;
	
	public InvalidConfigException(String msg) {
		super(msg);
	}

}
