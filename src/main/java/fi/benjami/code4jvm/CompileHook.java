package fi.benjami.code4jvm;

import fi.benjami.code4jvm.block.Block;
import fi.benjami.code4jvm.typedef.ClassDef;

/**
 * Compile hooks {@link Block#setCompileHook(Object, CompileHook) added to}
 * blocks can be used to execute code when the class definition containing
 * them is executed.
 *
 */
public interface CompileHook {
	
	interface Carrier {
		void setCompileHook(Object key, CompileHook hook);
	}

	void onCompile(ClassDef def);
}
