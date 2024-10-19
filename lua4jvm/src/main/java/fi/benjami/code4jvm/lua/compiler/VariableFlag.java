package fi.benjami.code4jvm.lua.compiler;

import fi.benjami.code4jvm.lua.ir.LuaLocalVar;

/**
 * Flags that can be assigned to {@link LuaLocalVar local variables} in
 * {@link LuaContext compiler contexts}. Thus, these are unique per compilation
 * run.
 */
public enum VariableFlag {
	
	/**
	 * Variable is assigned to once or more.
	 */
	ASSIGNED(null),
	
	/**
	 * Variable is mutable; that is, it is assigned to at least twice.
	 */
	MUTABLE(CompilerPass.TYPE_ANALYSIS)
	;
	
	/**
	 * The flag must not be checked during this pass, but can be set.
	 * Null to disable this check. When assertions are enabled,
	 * {@link LuaContext#hasFlag(LuaLocalVar, VariableFlag)} checks this.
	 * 
	 * <p>Locked passes are used to ensure that flags are not being mutated
	 * and read at the same time. If unintentional, such actions could lead to
	 * subtle but nasty bugs.
	 */
	public final CompilerPass lockedPass;
	
	VariableFlag(CompilerPass lockedPass) {
		this.lockedPass = lockedPass;
	}
}
