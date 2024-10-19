package fi.benjami.code4jvm.lua.ir;

import fi.benjami.code4jvm.lua.runtime.LuaFunction;

@SuppressWarnings("unused") // Javadoc import
public record UpvalueTemplate(
		/**
		 * The variable that represents this inside the function that
		 * captures it.
		 */
		LuaLocalVar variable,
		
		/**
		 * Preliminary type of the upvalue. Compared to
		 * {@link LuaFunction#upvalueTypes final types} that are known after
		 * the function has been instantiated, this may be unknown.
		 */
		LuaType type,
		
		/**
		 * Whether or not the upvalue variable is assigned to after its initial
		 * assignment.
		 */
		boolean mutable
) {}
