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
			var types = new LuaType[] {LuaType.TABLE, LuaType.UNKNOWN};
			var target = LuaLinker.linkCall(new LuaCallSite(null, new CallSiteOptions(types, false, false)), index, this, key);
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
				var types = new LuaType[] {LuaType.TABLE, LuaType.UNKNOWN, LuaType.UNKNOWN};
				var target = LuaLinker.linkCall(new LuaCallSite(null, new CallSiteOptions(types, false, false)), newIndex, this, key, value);
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
			if (actualKey == null || key.equals(actualKey)) {
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
		
		var newCapacity = Math.max(4, (int) (arrayCapacity * 1.3f));
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
}
