package fi.benjami.code4jvm.internal;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

import fi.benjami.code4jvm.block.Block;
import fi.benjami.code4jvm.block.BlockPrinter;

/**
 * Internal tools for generating debug names.
 * 
 * @see BlockPrinter
 *
 */
public final class DebugNames {
	
	/**
	 * A simple name generator that uses the given prefix and a counter.
	 *
	 */
	public static class Counting {
		
		private final String prefix;
		private final Map<Object, String> debugNames;
		private int nextName;
		
		public Counting(String prefix) {
			this.prefix = prefix;
			this.debugNames = new IdentityHashMap<>();
		}
		
		public String make(Object obj) {
			return debugNames.computeIfAbsent(obj, k -> prefix + nextName++);
		}
	}
	
	/**
	 * Block debug name generator. In general, debug names are only generated
	 * if user-specified names are missing. However, block names may have
	 * duplicates within a single method that need to be distinguished from
	 * each other.
	 *
	 */
	public static class ForBlocks {
		
		private final Map<Block, String> names;
		private final Map<String, Integer> numbers;
		
		public ForBlocks() {
			this.names = new IdentityHashMap<>();
			this.numbers = new HashMap<>();
		}
		
		public String make(Block block, String name) {
			return names.computeIfAbsent(block, k -> {
				var num = numbers.compute(name, (k2, prev) -> {
					return prev == null ? 0 : prev + 1;
				});
				return name + " #" + num;
			});
		}
	}

}
