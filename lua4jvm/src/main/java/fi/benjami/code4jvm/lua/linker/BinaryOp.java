package fi.benjami.code4jvm.lua.linker;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.function.BiFunction;

import fi.benjami.code4jvm.lua.ir.LuaType;
import fi.benjami.code4jvm.lua.runtime.LuaTable;
import fi.benjami.code4jvm.lua.runtime.TableAccess;
import fi.benjami.code4jvm.lua.stdlib.LuaException;

/**
 * Linker support for Lua's binary operations, such as addition, multiplication
 * or string concatenation. Both LHS and RHS of these operations can use
 * metatables to affect their behavior, which makes implementing them with
 * unknown types somewhat difficult.
 *
 */
public class BinaryOp {
	
	private static final boolean checkTypes(Class<?> expected, Object callable, Object lhs, Object rhs) {
		return lhs != null && lhs.getClass() == expected && rhs != null && rhs.getClass() == expected;
	}
	
	@SuppressWarnings("unused") // MethodHandle
	private static final boolean checkLhsMetamethod(String metamethod, Object callable, Object lhs) {
		if (!(lhs instanceof LuaTable table)) {
			return true; // Can't possibly have a metamethod
		}
		return table.metatable() != null && table.metatable().get(metamethod) != null;
	}
	
	private static final MethodHandle CHECK_TYPES, CHECK_LHS_METAMETHOD;
	
	static {
		var lookup = MethodHandles.lookup();
		try {
			CHECK_TYPES = lookup.findStatic(BinaryOp.class, "checkTypes",
					MethodType.methodType(boolean.class, Class.class, Object.class, Object.class, Object.class));
			CHECK_LHS_METAMETHOD = lookup.findStatic(BinaryOp.class, "checkLhsMetamethod",
					MethodType.methodType(boolean.class, String.class, Object.class, Object.class));
		} catch (NoSuchMethodException | IllegalAccessException e) {
			throw new AssertionError(e);
		}
	}
	
	/**
	 * Produces a dynamic call target for a binary operation call site.
	 * @param expectedType Type that the fast path supports.
	 * @param fastPath Fast path that is entered if both sides are of expected
	 * type. The fast path should accept the call target as first parameter,
	 * LHS as second and RHS as third.
	 * @param metamethod Name of the metamethod call if metatables are present.
	 * @param errorHandler Called when either value has invalid type and
	 * metamethods are not found. Returns a Lua exception that is thrown.
	 * @return Call target.
	 */
	public static DynamicTarget newTarget(Class<?> expectedType, MethodHandle fastPath, String metamethod,
			BiFunction<Object, Object, LuaException> errorHandler) {
		assert !expectedType.isPrimitive(); // LHS and RHS will be in their boxed forms
		assert !expectedType.equals(LuaType.class); // This is currently unnecessary for Lua
		return (meta, args) -> {
			assert args.length == 2;
			var lhs = args[0];
			var rhs = args[1];
			if (checkTypes(expectedType, null, lhs, rhs)) {
				// Fast path, e.g. arithmetic operation on numbers or string concatenation on strings
				var guard = CHECK_TYPES.bindTo(expectedType);
				return new LuaCallTarget(fastPath, guard);
			} else if (lhs instanceof LuaTable table
					&& table.metatable() != null
					&& table.metatable().get(metamethod) != null) {
				// Slower path, call LHS metamethod
				return useMetamethod(meta, table, metamethod, lhs, rhs);
			} else if (rhs instanceof LuaTable table
					&& table.metatable() != null
					&& table.metatable().get(metamethod) != null) {
				// Call RHS metamethod like above, but also relink if LHS ever gets a metamethod
				var target = useMetamethod(meta, table, metamethod, lhs, rhs);
				var guard = CHECK_LHS_METAMETHOD.bindTo(metamethod);
				return target.withGuards(guard);
			} else {
				throw errorHandler.apply(lhs, rhs);
			}
		};
	}
	
	private static LuaCallTarget useMetamethod(LuaCallSite meta, LuaTable table, String metamethod, Object lhs, Object rhs) {
		var target = LuaLinker.linkCall(new LuaCallSite(meta.site,
				CallSiteOptions.nonFunction(meta.options.owner(), LuaType.UNKNOWN, LuaType.UNKNOWN)),
				table.metatable().get(metamethod), lhs, rhs);
		var guard = MethodHandles.insertArguments(TableAccess.CHECK_TABLE_AND_META_SHAPES, 0,
				table.shape(), table.metatable().shape());
		return target.withGuards(guard);
	}
	
}
