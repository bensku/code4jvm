package fi.benjami.code4jvm.lua.ir.expr;

import java.util.function.BiFunction;

import fi.benjami.code4jvm.Expression;
import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.Value;
import fi.benjami.code4jvm.block.Block;
import fi.benjami.code4jvm.call.CallTarget;
import fi.benjami.code4jvm.lua.compiler.LuaContext;
import fi.benjami.code4jvm.lua.ir.IrNode;
import fi.benjami.code4jvm.lua.ir.LuaType;
import fi.benjami.code4jvm.lua.runtime.LuaOps;
import fi.benjami.code4jvm.statement.Arithmetic;

/**
 * An expression that represents an arithmetic operation supported by Lua.
 *
 */
public record ArithmeticExpr(
		IrNode lhs,
		Kind kind,
		IrNode rhs
) implements IrNode {

	private static final CallTarget MATH_POW = CallTarget.staticMethod(Type.of(Math.class), Type.DOUBLE, "pow", Type.DOUBLE, Type.DOUBLE);
	
	public enum Kind {
		POWER(MATH_POW::call, LuaOps.POWER),
		MULTIPLY(Arithmetic::multiply, LuaOps.MULTIPLY),
		DIVIDE(Arithmetic::divide, LuaOps.DIVIDE),
		FLOOR_DIVIDE((lhs, rhs) -> null, null), // TODO read spec, Math#floorDiv(int, int) could be useful
		MODULO(Arithmetic::remainder, LuaOps.MODULO), // TODO verify that JVM and Lua specs define this same way
		ADD(Arithmetic::add, LuaOps.ADD),
		SUBTRACT(Arithmetic::subtract, LuaOps.SUBTRACT);
		
		private BiFunction<Value, Value, Expression> fastPath;
		private CallTarget slowPath;
		
		Kind(BiFunction<Value, Value, Expression> fastPath, CallTarget slowPath) {
			this.fastPath = fastPath;
			this.slowPath = slowPath;
		}
	}
	
	@Override
	public Value emit(LuaContext ctx, Block block) {
		var lhsValue = lhs.emit(ctx, block);
		var rhsValue = rhs.emit(ctx, block);
		if (!LuaType.of(lhsValue).equals(LuaType.NUMBER) || !LuaType.of(rhsValue).equals(LuaType.NUMBER)) {
			// Either lhs or rhs is not known to be number -> take slow path and check
			return block.add(kind.slowPath.call(lhsValue.cast(Type.OBJECT), rhsValue.cast(Type.OBJECT)));
		} else {
			// Simple, fast arithmetic
			return block.add(kind.fastPath.apply(lhsValue, rhsValue));
		}
	}
	
	@Override
	public LuaType outputType(LuaContext ctx) {
		return lhs.outputType(ctx).equals(LuaType.NUMBER) && rhs.outputType(ctx).equals(LuaType.NUMBER)
				? LuaType.NUMBER : LuaType.UNKNOWN;
	}

}
