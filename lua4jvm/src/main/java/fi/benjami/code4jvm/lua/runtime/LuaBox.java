package fi.benjami.code4jvm.lua.runtime;

import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.lua.ir.expr.VariableExpr;
import fi.benjami.code4jvm.lua.ir.stmt.SetVariablesStmt;

/**
 * A mutable boxed value. Used for implementing mutable Lua upvalues, because
 * JVM local variables cannot be shared between methods that way.
 * 
 * Most code never sees boxes: {@link SetVariablesStmt} transparently creates
 * and {@link VariableExpr} unpacks the value.
 */
public class LuaBox {
	
	public static final Type TYPE = Type.of(LuaBox.class);

	/**
	 * Generated bytecode does direct field access on this.
	 */
	public Object value;
}
