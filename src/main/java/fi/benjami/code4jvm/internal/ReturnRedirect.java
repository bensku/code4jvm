package fi.benjami.code4jvm.internal;

import org.objectweb.asm.Label;

public record ReturnRedirect(
		/**
		 * Where to jump when return is encountered
		 */
		Label target,
		
		/**
		 * Local variable where return value should be stored. Null if method
		 * returns void.
		 */
		LocalVar returnValue
) {
	
	public ReturnRedirect {
		// We don't want to mutate caller-provided return value here
		// So just make sure it is usable by ReturnNode later
		assert returnValue.initialized;
		assert returnValue.needsSlot;
	}
}
