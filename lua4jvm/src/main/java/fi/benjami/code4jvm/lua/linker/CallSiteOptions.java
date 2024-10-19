package fi.benjami.code4jvm.lua.linker;

import java.util.Arrays;

import fi.benjami.code4jvm.lua.LuaVm;
import fi.benjami.code4jvm.lua.ir.LuaType;

public record CallSiteOptions(
		/**
		 * Owner of the call site.
		 */
		LuaVm owner,
		
		/**
		 * Argument types of the call site. These are the compile-time types,
		 * which may include UNKNOWNs.
		 */
		LuaType[] types,
		
		/**
		 * True if the call site wants to receive a multival, false
		 * otherwise. If this is false and the target would return
		 * a multival, the target should instead return the first value of
		 * the multival. If this is true, the target may return multival
		 * or a single value and the caller must account for that.
		 */
		boolean spreadResults,
		
		/**
		 * True if the call site wants to use another function call or '...'
		 * as the its last argument. This forces the linker to inspect the
		 * arguments and (attempt to) spread it over arguments.
		 */
		boolean spreadArguments,
		
		/**
		 * True if the call target is known compile-time and will not change.
		 * In some cases, this may allow using constant call sites or even not
		 * using invokedynamic at all.
		 */
		boolean stableTarget,
		
		/**
		 * Intrinsic id of this call site. If non-null, when the call target is
		 * a Java function, its targets with same intrinsic id are selected in
		 * addition to 
		 */
		String intrinsicId
) {
	
	public CallSiteOptions(LuaVm owner, LuaType[] types, boolean spreadResults, boolean spreadArguments, boolean stableTarget) {
		this(owner, types, spreadResults, spreadArguments, stableTarget, null);
	}
	
	/**
	 * Creates call site options for a non-function call.
	 * @param types Types at call site. Use UNKNOWNs if not known or needed.
	 * @return Call site options.
	 */
	public static CallSiteOptions nonFunction(LuaVm vm, LuaType... types) {
		return new CallSiteOptions(vm, types, false, false, false);
	}
	
	public CallSiteOptions wrappedCall(int popArgs) {
		var innerTypes = Arrays.copyOfRange(types, popArgs, types.length - popArgs + 1);
		return new CallSiteOptions(owner, innerTypes, spreadResults, spreadArguments, false);
	}

}
