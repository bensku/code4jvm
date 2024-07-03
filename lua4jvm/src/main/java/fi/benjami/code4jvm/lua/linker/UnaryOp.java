package fi.benjami.code4jvm.lua.linker;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.function.Function;

import fi.benjami.code4jvm.lua.ir.LuaType;
import fi.benjami.code4jvm.lua.runtime.LuaTable;
import fi.benjami.code4jvm.lua.runtime.TableAccess;
import fi.benjami.code4jvm.lua.stdlib.LuaException;

/**
 * Linker support for Lua's unary operations such as negation.
 * Supports metamethods for operator overloading.
 * @see BinaryOp
 */
public class UnaryOp {
	
	private static final boolean checkType(Class<?> expected, Object callable, Object arg) {
		return arg != null && arg.getClass() == expected;
	}
	
	private static final MethodHandle CHECK_TYPE;
	
	static {
		var lookup = MethodHandles.lookup();
		try {
			CHECK_TYPE = lookup.findStatic(UnaryOp.class, "checkType",
					MethodType.methodType(boolean.class, Class.class, Object.class, Object.class));
		} catch (NoSuchMethodException | IllegalAccessException e) {
			throw new AssertionError(e);
		}
	}
	
	public record Path(Class<?> type, MethodHandle target) {}
	
	/**
	 * Produces a dynamic call target for an unary operation call site.
	 * @param fastPaths Fast paths to check in order.
	 * @param metamethod Name of the metamethod to call. When present, this
	 * takes precedence over the fast path!
	 * @param errorHandler Called when the argument has invalid type and
	 * metamethod cannot be found. Returns a Lua exception that is thrown.
	 * @return Call target.
	 */
	public static DynamicTarget newTarget(Path[] fastPaths, String metamethod,
			Function<Object, LuaException> errorHandler) {
		return (meta, args) -> {
			var arg = args[0];
			// Try all paths in order
			for (var path : fastPaths) {
				if (path.type.equals(LuaTable.class)) {
					if (arg instanceof LuaTable table) {
						if (table.metatable() == null) {
							// Fast path: no metatable
							var guard = TableAccess.CHECK_TABLE_SHAPE.bindTo(table.shape());
							return new LuaCallTarget(path.target, guard);
						} else if (table.metatable().get(metamethod) == null) {
							// Metatable, but no relevant metamethod
							var guard = MethodHandles.insertArguments(TableAccess.CHECK_TABLE_AND_META_SHAPES, 0,
									table.shape(), table.metatable().shape());
							return new LuaCallTarget(path.target, guard);
						} else {
							// Metamethod found; call it!
							return useMetamethod(meta, table, metamethod, arg);
						}
					}
				} else {
					if (checkType(path.type, null, arg)) {
						// Expected type; take the fast path until this changes
						var guard = CHECK_TYPE.bindTo(path.type);
						return new LuaCallTarget(path.target, guard);
					} else if (arg instanceof LuaTable table
							&& table.metatable() != null
							&& table.metatable().get(metamethod) != null) {
						// Unexpected type, but we can call the metamethod
						return useMetamethod(meta, table, metamethod, arg);
					}
				}
			}
			throw errorHandler.apply(arg);
		};
	}
	
	private static LuaCallTarget useMetamethod(LuaCallSite meta, LuaTable table, String metamethod, Object arg) {
		var target = LuaLinker.linkCall(new LuaCallSite(meta.site, CallSiteOptions.nonFunction(meta.options.owner(), LuaType.UNKNOWN)),
				table.metatable().get(metamethod), arg);
		var guard = MethodHandles.insertArguments(TableAccess.CHECK_TABLE_AND_META_SHAPES, 0,
				table.shape(), table.metatable().shape());
		return target.withGuards(guard);
	}
	
}
