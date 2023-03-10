package fi.benjami.parserkit.parser.internal;

import fi.benjami.code4jvm.Constant;
import fi.benjami.code4jvm.Statement;
import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.Value;
import fi.benjami.code4jvm.block.Block;
import fi.benjami.parserkit.parser.CompileError;

public class ErrorManager {
	
	private static final Type COMPILE_ERROR = Type.of(CompileError.class);
		
	public static ErrorManager initialize(Block block) {
		var list = block.add(ParserGenerator.HASH_SET.newInstance());
		return new ErrorManager(list);
	}

	private final Value errorList;
	
	public ErrorManager(Value errorList) {
		this.errorList = errorList;
	}
	
	public Statement errorAtToken(int type, Value token) {
		return block -> {
			var start = block.add(token.callVirtual(Type.INT, "start"));
			var end = block.add(token.callVirtual(Type.INT, "end"));
			var error = block.add(COMPILE_ERROR.newInstance(Constant.of(type), start, end));
			block.add(ParserGenerator.SET_ADD.call(errorList, error));
		};
	}
	
	public Statement errorAtHere(int type, Value tokenView) {
		return block -> {
			var offset = block.add(tokenView.callVirtual(Type.INT, "textOffset"));
			var error = block.add(COMPILE_ERROR.newInstance(Constant.of(type), offset, offset));
			block.add(ParserGenerator.SET_ADD.call(errorList, error));				
		};
	}
	
	public Value errorSet() {
		return errorList;
	}
}
