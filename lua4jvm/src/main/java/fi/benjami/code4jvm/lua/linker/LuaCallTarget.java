package fi.benjami.code4jvm.lua.linker;

import java.lang.invoke.MethodHandle;

/**
 * A call target for dynamic linkage.
 * 
 * @see LuaLinker
 *
 */
public record LuaCallTarget(
		/**
		 * Method to call.
		 */
		MethodHandle target,
		
		/**
		 * Guards that should be evaluated before calling the target.
		 * If they fail (by returning false), the call site is relinked.
		 * 
		 * <p>Multiple guards can be stacked on top of each other. The
		 * last guard in this array becomes the outermost one.
		 */
		MethodHandle... guards
) {
	
	public LuaCallTarget withGuards(MethodHandle... appendGuards) {
		var allGuards = new MethodHandle[guards.length + appendGuards.length];
		System.arraycopy(guards, 0, allGuards, 0, guards.length);
		System.arraycopy(appendGuards, 0, allGuards, guards.length, appendGuards.length);
		return new LuaCallTarget(target, allGuards);
	}
}
