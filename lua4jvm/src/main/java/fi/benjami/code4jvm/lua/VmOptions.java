package fi.benjami.code4jvm.lua;

import java.io.PrintStream;
import java.util.Collection;
import java.util.List;

import fi.benjami.code4jvm.lua.ffi.LuaLibrary;
import fi.benjami.code4jvm.lua.stdlib.BasicLib;

/**
 * lua4jvm Lua VM options.
 *
 */
public class VmOptions implements Cloneable {
	
	public static final VmOptions DEFAULT = new VmOptions();
	
	public static final Builder builder() {
		return new Builder();
	}

	private Collection<LuaLibrary> libraries = List.of(BasicLib.INSTANCE);
	private PrintStream stdOut = System.out;
	
	private VmOptions() {}
	
	public static class Builder {
		
		private VmOptions opts;
		
		private Builder() {
			this.opts = new VmOptions();
		}
		
		public Builder libraries(LuaLibrary... libraries) {
			opts.libraries = List.of(libraries);
			return this;
		}
		
		public Builder stdOut(PrintStream out) {
			opts.stdOut = out;
			return this;
		}
		
		public VmOptions build() {
			try {
				return (VmOptions) opts.clone();
			} catch (CloneNotSupportedException e) {
				throw new AssertionError(e);
			}
		}
	}
	
	public Collection<LuaLibrary> libraries() {
		return libraries;
	}
	
	public PrintStream stdOut() {
		return stdOut;
	}
}
