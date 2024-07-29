package fi.benjami.code4jvm.lua.runtime;

import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.lua.ir.LuaType;
import fi.benjami.code4jvm.lua.linker.CallSiteOptions;
import fi.benjami.code4jvm.lua.linker.LuaCallSite;
import fi.benjami.code4jvm.lua.linker.LuaLinker;

/**
 * A Java implementation of the Lua <code>table</code> data type.
 *
 * @implNote The table part uses open addressing (closed hashing), while
 * array part is just an array. Both are internally placed within same Java
 * array.
 */
public class LuaTable {
	
	public static final Type TYPE = Type.of(LuaTable.class);
	
	private static final Object[] EMPTY = new Object[0];
	private static final Object TOMBSTONE = new Object();

	private Object[] table;
	private int arraySize, arrayCapacity;
	private Object[] keys;
	LuaTable metatable;
	
	Object shape;
	
	public LuaTable() {
		this.table = EMPTY;
		this.keys = EMPTY;
	}
	
	private static int hash(Object key) {
		int hash;
		return key == null ? 0 : (hash = key.hashCode()) ^ (hash >>> 16);
	}
	
	int getSlot(Object key) {
		if (keys == EMPTY) {
			return -1;
		}
		var slot = hash(key) & (keys.length - 1);
		for (; slot < keys.length; slot++) {
			var actualKey = keys[slot];
			// TODO consider storing hashes and checking against them; equality checks may be slow
			if (actualKey == null) {
				return -1;
			} else if (key.equals(actualKey)) {
				return slot;
			}
		}
		return -1;
	}
	
	int getArrayIndex(Object key) {
		if (key instanceof Double num) {
			var index = num.intValue();
			if (index == num.doubleValue() && index < arrayCapacity) {
				return index;
			}
		}
		return -1; // Not an array index
	}
	
	// Getters
	
	Object getArray(int index) {
		assert index < arraySize;
		return table[index];
	}
	
	Object getAt(int slot) {
		return table[arrayCapacity + slot];
	}
	
	public Object getRaw(Object key) {
		var arrayIndex = getArrayIndex(key);
		if (arrayIndex != -1) {
			return table[arrayIndex];
		}
		
		var slot = getSlot(key);
		if (slot == -1) {
			return null;
		}
		return table[arrayCapacity + slot];
	}
	
	public Object get(Object key) {
		var value = getRaw(key);
		if (value != null || metatable == null) {
			return value;
		}
		
		var index = metatable.get("__index");
		if (index == null) {
			return null;
		}
		if (index instanceof LuaTable table) {
			return table.get(key);
		} else {
			// Executable __index
			// TODO try to optimize this using LambdaMetafactory or custom codegen
			var target = LuaLinker.linkCall(new LuaCallSite(null, CallSiteOptions.nonFunction(null, LuaType.TABLE, LuaType.UNKNOWN)), index, this, key);
			try {
				return target.target().invoke(index, this, key);
			} catch (Throwable e) {
				throw new RuntimeException(e); // TODO Lua error handling
			}
		}
	}
	
	// Setters
	
	void setArray(int index, Object value) {
		table[index] = value;
	}
	
	void setAt(int slot, Object key, Object value) {
		var oldKey = keys[slot];
		if (value == null && oldKey != null) {
			// If a key is removed from table, a different key could be placed to same slot
			// Additionally, this affects __index and __newindex if those exist
			shapeChanged();
			key = TOMBSTONE; // We might be punching a hole to a cluster of hash collisions
		} else if (metatable != null && oldKey == null) {
			// If an entirely new key is written AND we have a metatable
			// get/set on that key will no longer be sent to __index/__newindex
			shapeChanged();
		} else if (shape instanceof MetatableShape) {
			shapeChanged();
		}
		
		if (oldKey == null) {			
			keys[slot] = key; // Add new key to table
		} else if (value == null) {
			keys[slot] = null; // Delete key from table
		}
		table[arrayCapacity + slot] = value;
	}
	
	public void setRaw(Object key, Object value) {
		if (key instanceof Double num) {
			// The logic here is subtly different from getArrayIndex()
			// We allow appending to array (with a few gaps) even if it is full
			var integer = num.intValue();
			if (integer == num.doubleValue() && integer < arrayCapacity + 3) {
				if (integer >= arrayCapacity) {
					enlargeArray();
				}
				table[integer] = value;
				arraySize = Math.max(arraySize, integer + 1);
				return;
			}
		}
		
		var success = setToTable(key, value);
		while (!success) {
			// Enlarge array until we have space and/or less collisions
			// TODO handle MANY hash collisions somehow (e.g. table-specific maxCollisions?)
			enlargeTables();
			success = setToTable(key, value);
		}
	}
	
	public void set(Object key, Object value) {
		if (metatable != null && getRaw(key) == null) {
			// Metatable present and new key -> try to call __newindex
			var newIndex = metatable.get("__newindex");
			if (newIndex == null) {
				setRaw(key, value);
				return;
			}
			if (newIndex instanceof LuaTable table) {
				table.set(key, value);
			} else {
				// Executable __newindex
				// TODO try to optimize this using LambdaMetafactory or custom codegen
				var target = LuaLinker.linkCall(new LuaCallSite(null, CallSiteOptions.nonFunction(null, LuaType.TABLE, LuaType.UNKNOWN, LuaType.UNKNOWN)),
						newIndex, this, key, value);
				try {
					target.target().invoke(newIndex, this, key, value);
				} catch (Throwable e) {
					throw new RuntimeException(e); // TODO Lua error handling
				}
			}
		} else {			
			setRaw(key, value);
		}
	}
	
	int getFreeSlot(Object key) {
		if (keys == EMPTY) {
			return -1;
		}
		var slot = hash(key) & (keys.length - 1);
		for (; slot < keys.length; slot++) {
			var actualKey = keys[slot];
			if (actualKey == null || actualKey == TOMBSTONE || key.equals(actualKey)) {
				return slot;
			}
		}
		return -1;
	}
	
	private boolean setToTable(Object key, Object value) {
		var free = getFreeSlot(key);
		if (free == -1) {
			return false;
		}
		setAt(free, key, value);
		return true;
	}
	
	// Internal array management
	
	private void enlargeTables() {
		shapeChanged();
		
		var newSlotCount = Math.max(4, keys.length * 2);
		var oldTable = table;
		var oldKeys = keys;
		this.table = new Object[arrayCapacity + newSlotCount];
		System.arraycopy(oldTable, 0, table, 0, arraySize); // Copy array members
		this.keys = new Object[newSlotCount];
		
		// Re-insert all non-array elements
		// TODO this may be terribly slow
		for (var i = 0; i < oldKeys.length; i++) {
			var k = oldKeys[i];
			if (k != null) {
				setToTable(k, oldTable[arrayCapacity + i]);
			}
		}
	}
	
	private void enlargeArray() {
		shapeChanged();
		
		// TODO don't always double capacity; but make sure this always adds at least 3 slots (to account for gaps)!
		var newCapacity = Math.max(4, arrayCapacity * 2);
		var newTable = new Object[newCapacity + keys.length];
		
		// Copy old array part
		System.arraycopy(table, 0, newTable, 0, arraySize);
		// Copy old table part, but leave space for expanded capacity
		System.arraycopy(table, arrayCapacity, newTable, newCapacity, keys.length);
		
		// Scan rest of the table to find if any keys should be moved to array part
		for (var i = 0; i < keys.length; i++) {
			var key = keys[i];
			if (key instanceof Double d) {
				var integer = d.intValue();
				if (integer == d.doubleValue() && integer < newCapacity) {
					// Move to array part
					newTable[integer] = table[arrayCapacity + i];
					table[arrayCapacity + i] = null;
					keys[i] = null;
				}
			}
		}
		
		this.arrayCapacity = newCapacity;
		this.table = newTable;
	}
	
	private void shapeChanged() {
		if (shape instanceof MetatableShape) {
			this.shape = new MetatableShape();
		} else {			
			this.shape = new Object();
		}
	}
	
	// Misc
	
	public LuaTable metatable() {
		return metatable;
	}
	
	public void metatable(LuaTable metatable) {
		if (this.metatable == metatable) {
			return; // Metatable didn't actually change
		}
		this.metatable = metatable;
		shapeChanged();
	}
	
	/**
	 * Marks this table as being used as a metatable.
	 */
	void markAsMetatable() {
		// In future, MetatableShape may gain members - that's why it is used instead of simple flag
		this.shape = new MetatableShape();
	}
	
	public int arraySize() {
		// Lua doesn't count table[0] as part of array
		// For simplicity, we still keep it as first slot of the array
		return arraySize == 0 ? 0 : arraySize - 1;
	}
	
	public Object shape() {
		return shape;
	}
	
	/**
	 * Gets the next entry in this table.
	 * 
	 * <p>This method supports stateless iteration, much like Lua's next()
	 * function. When writing Java, this is probably not what you want!
	 * Stateful iterators produced by {@link #iterator()} are more efficient,
	 * potentially significantly so.
	 * @param prevKey Key of the previous entry, or null to start from scratch.
	 * @return A pair of key and value.
	 */
	public Object[] next(Object prevKey) {
		if (prevKey == null) {
			if (arraySize != 0) {				
				// First call, array has at least one member
				return new Object[] {1d, getArray(1)};
			} else {
				// First call, no array members -> return "first" table member
				for (var i = arrayCapacity; i < table.length; i++) {
					if (table[i] != null) {
						return new Object[] {keys[i - arrayCapacity], table[i]};
					}
				}
			}
		}
		
		int slot;
		if (prevKey instanceof Double index) {
			// Iterate the array in order as long as we have elements
			if (index < arraySize - 1) {				
				var newIndex = index + 1;
				return new Object[] {newIndex, getArray((int) newIndex)};
			} else {
				slot = 0; // First entry after array part
			}
		} else {
			slot = getSlot(prevKey) + 1;
		}
		
		// Out of array members
		for (var i = slot + arrayCapacity; i < table.length; i++) {
			if (table[i] != null) {
				return new Object[] {keys[i - arrayCapacity], table[i]};
			}
		}
		return null; // Table end
	}
	
	/**
	 * Creates a stateful table iterator of this table.
	 * 
	 * <p><b>Note:</b> Table iterators are not compatible with
	 * {@link java.util.Iterator}!
	 * @return A table iterator.
	 */
	public Iterator iterator() {
		return new Iterator(false);
	}
	
	/**
	 * Creates a stateful table iterator that only iterates through the array part.
	 * @return A table iterator.
	 */
	public Iterator arrayIterator() {
		return new Iterator(true);
	}
	
	/**
	 * A stateful table iterator.
	 * 
	 * <p>Typical usage:
	 * <pre>
	 * var it = table.iterator();
	 * while (it.next()) {
	 *     var key = it.key();
	 *     var value = it.value();
	 * }
	 * </pre>
	 *
	 */
	public class Iterator {
		
		private final boolean arrayOnly;
		private boolean array;
		private int index;
		
		private Iterator(boolean arrayOnly) {
			this.arrayOnly = arrayOnly;
			this.array = true;
			this.index = 0;
		}
		
		public boolean next() {
			return array ? nextArray() : nextTable();
		}
		
		private boolean nextArray() {
			index++;
			if (index >= arraySize || table[index] == null) {
				// Reached array end
				// ... but if there were previously gaps, some hash table entries
				// might also need to be visible to array iterators
				var nextEntry = getRaw((double) index);
				if (nextEntry != null) {
					return true;
				} // else: REALLY reached array end
				
				array = false;
				index = arrayCapacity - 1; // Jump over possible unused array space
				return nextTable(); // Table might or might not have entries
			}
			return true;
		}
		
		private boolean nextTable() {
			if (arrayOnly) {
				return false; // ipairs() like behavior
			}
			
			// Iterate over empty space until we find next entry
			for (var i = index + 1; i < table.length; i++) {
				if (table[i] != null) {
					index = i;
					return true;
				}
			}
			index = table.length;
			return false;
		}
		
		public Object key() {
			return array ? (double) index : keys[index - arrayCapacity];
		}
		
		public Object value() {
			return table[index];
		}
	}
}
