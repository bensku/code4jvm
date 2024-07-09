package fi.benjami.code4jvm.lua;

import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.FileSystem;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

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
	private Optional<PrintStream> stdOut = Optional.of(System.out);
	private Optional<InputStream> stdIn = Optional.empty(); // By default, don't let VM capture user input!
	private Optional<FileSystem> fileSystem = Optional.empty();
	
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
			opts.stdOut = Optional.of(out);
			return this;
		}
		
		public Builder stdIn(InputStream in) {
			opts.stdIn = Optional.of(in);
			return this;
		}
		
		public Builder fileSystem(FileSystem fs) {
			opts.fileSystem = Optional.of(fs);
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
	
	public Optional<PrintStream> stdOut() {
		return stdOut;
	}
	
	public Optional<InputStream> stdIn() {
		return stdIn;
	}
	
	public Optional<FileSystem> fileSystem() {
		return fileSystem;
	}
}
