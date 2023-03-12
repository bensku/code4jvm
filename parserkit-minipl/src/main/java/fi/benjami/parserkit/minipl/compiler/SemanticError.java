package fi.benjami.parserkit.minipl.compiler;

import java.util.List;

public record SemanticError(
		Type type,
		int start,
		int end,
		List<Object> fmtArgs
) {
	
	public static SemanticError of(Type type, Object... fmtArgs) {
		return new SemanticError(type, -1, -1, List.of(fmtArgs));
	}
	
	public enum Type {
		UNKNOWN_TYPE("variable %s has unknown type %s"),
		VARIABLE_ALREADY_DEFINED("variable %s is already defined"),
		VARIABLE_NOT_DEFINED("variable %s is not defined"),
		TYPE_CONFLICT("types %s and %s are not compatible");
		
		private final String formatStr;
		
		Type(String formatStr) {
			this.formatStr = formatStr;
		}
		
		public String errorMsg(List<Object> fmtArgs) {
			return formatStr.formatted(fmtArgs.toArray());
		}
	}
}
