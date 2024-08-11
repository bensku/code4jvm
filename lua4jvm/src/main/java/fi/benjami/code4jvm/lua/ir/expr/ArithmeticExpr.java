package fi.benjami.code4jvm.lua.ir.expr;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;
import java.util.function.BiFunction;

import fi.benjami.code4jvm.Expression;
import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.Value;
import fi.benjami.code4jvm.block.Block;
import fi.benjami.code4jvm.call.CallTarget;
import fi.benjami.code4jvm.lua.compiler.LuaContext;
import fi.benjami.code4jvm.lua.ir.IrNode;
import fi.benjami.code4jvm.lua.ir.LuaType;
import fi.benjami.code4jvm.lua.linker.BinaryOp;
import fi.benjami.code4jvm.lua.linker.CallSiteOptions;
import fi.benjami.code4jvm.lua.linker.DynamicTarget;
import fi.benjami.code4jvm.lua.linker.LuaLinker;
import fi.benjami.code4jvm.lua.stdlib.LuaException;
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
	private static final CallTarget MATH_ABS = CallTarget.staticMethod(Type.of(Math.class), Type.DOUBLE, "abs", Type.DOUBLE);
	private static final CallTarget FLOOR_DIV = CallTarget.staticMethod(Type.of(ArithmeticExpr.class), Type.DOUBLE, "floorDivide", Type.DOUBLE, Type.DOUBLE);
	private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
	
	public enum Kind {
		POWER(MATH_POW::call, "power", "__pow"),
		MULTIPLY(Arithmetic::multiply, "multiply", "__mul"),
		DIVIDE((lhs, rhs) -> {
			// Lua uses float division unless integer division is explicitly request (see below)
			return Arithmetic.divide(lhs.cast(Type.DOUBLE), rhs.cast(Type.DOUBLE));
		}, "divide", "__div"),
		FLOOR_DIVIDE(FLOOR_DIV::call, "floorDivide", "__idiv"),
		MODULO((lhs, rhs) -> (block -> {
			// Lua expects modulo to be always positive; Java's remainder can return negative values
			var remainder = block.add(Arithmetic.remainder(lhs, rhs));
			return block.add(MATH_ABS.call(remainder));
		}), "modulo", "__mod"),
		ADD(Arithmetic::add, "add", "__add"),
		SUBTRACT(Arithmetic::subtract, "subtract", "__sub");
		
		private final BiFunction<Value, Value, Expression> directEmitter;
		private final DynamicTarget callTarget;
		
		Kind(BiFunction<Value, Value, Expression> directEmitter, String methodName, String metamethod) {
			this.directEmitter = directEmitter;
			var intReturnType = methodName == "power" || methodName.equals("divide") ? double.class : int.class;
			MethodHandle doublePath, intPath;
			try {
				// Drop the call target argument, it is not needed
				doublePath = MethodHandles.dropArguments(LOOKUP.findStatic(ArithmeticExpr.class, methodName,
						MethodType.methodType(double.class, double.class, double.class)), 0, Object.class);
				intPath = MethodHandles.dropArguments(LOOKUP.findStatic(ArithmeticExpr.class, methodName,
						MethodType.methodType(intReturnType, int.class, int.class)), 0, Object.class);
			} catch (NoSuchMethodException | IllegalAccessException e) {
				throw new AssertionError(e);
			}
			// If we have any doubles at all, take the double path
			var paths = List.of(
					new BinaryOp.Path(Integer.class, Integer.class, intPath),
					new BinaryOp.Path(Double.class, Double.class, doublePath),
					new BinaryOp.Path(Integer.class, Double.class, MethodHandles.explicitCastArguments(doublePath, MethodType.methodType(double.class, Object.class, int.class, double.class))),
					new BinaryOp.Path(Double.class, Integer.class, MethodHandles.explicitCastArguments(doublePath, MethodType.methodType(double.class, Object.class, double.class, int.class)))
			);
			this.callTarget = BinaryOp.newTarget(paths, metamethod,
					(a, b) -> new LuaException("cannot " + methodName + " "
							+ LuaType.of(a).name() + " and " + LuaType.of(b).name()));
		}
	}
	
	// MethodHandles
	
	@SuppressWarnings("unused")
	private static double power(double lhs, double rhs) {
		return Math.pow(lhs, rhs);
	}
	
	@SuppressWarnings("unused")
	private static double multiply(double lhs, double rhs) {
		return lhs * rhs;
	}
	
	@SuppressWarnings("unused")
	private static double divide(double lhs, double rhs) {
		return lhs / rhs;
	}
	
	public static double floorDivide(double lhs, double rhs) {
		return Math.floor(lhs / rhs); 
	}
	
	@SuppressWarnings("unused")
	private static double modulo(double lhs, double rhs) {
		return Math.abs(lhs % rhs);
	}
	
	@SuppressWarnings("unused")
	private static double add(double lhs, double rhs) {
		return lhs + rhs;
	}
	
	@SuppressWarnings("unused")
	private static double subtract(double lhs, double rhs) {
		return lhs - rhs;
	}
	
	@SuppressWarnings("unused")
	private static double power(int lhs, int rhs) {
		return Math.pow(lhs, rhs);
	}
	
	@SuppressWarnings("unused")
	private static int multiply(int lhs, int rhs) {
		return lhs * rhs;
	}
	
	private static double divide(int lhs, int rhs) {
		return ((double) lhs) / ((double) rhs);
	}
	
	public static int floorDivide(int lhs, int rhs) {
		return (int) Math.floor(divide(lhs, rhs)); 
	}
	
	@SuppressWarnings("unused")
	private static int modulo(int lhs, int rhs) {
		return Math.abs(lhs % rhs);
	}
	
	@SuppressWarnings("unused")
	private static int add(int lhs, int rhs) {
		return lhs + rhs;
	}
	
	@SuppressWarnings("unused")
	private static int subtract(int lhs, int rhs) {
		return lhs - rhs;
	}
	
	@Override
	public Value emit(LuaContext ctx, Block block) {
		var lhsValue = lhs.emit(ctx, block);
		var rhsValue = rhs.emit(ctx, block);
		if (outputType(ctx).isNumber()) {
			// Both arguments are known to be numbers; emit arithmetic operation directly
			return block.add(kind.directEmitter.apply(lhsValue, rhsValue));
		} else {
			// Types are unknown compile-time; use invokedynamic
			return block.add(LuaLinker.setupCall(ctx, CallSiteOptions.nonFunction(ctx.owner(), LuaType.UNKNOWN, LuaType.UNKNOWN), kind.callTarget, lhsValue, rhsValue));
		}
	}
	
	@Override
	public LuaType outputType(LuaContext ctx) {
		var lhsOut = lhs.outputType(ctx);
		var rhsOut = rhs.outputType(ctx);
		if (lhsOut.isNumber() && rhsOut.isNumber()) {
			if (kind == Kind.POWER || kind == Kind.DIVIDE) {
				// Lua spec says that these always produce floats
				return LuaType.FLOAT;
			} else if (lhsOut == LuaType.INTEGER && rhsOut == LuaType.INTEGER) {
				return LuaType.INTEGER; // Both sides are integers
			}
			return LuaType.FLOAT; // Float on at least one side
		} else {
			return LuaType.UNKNOWN;
		}
	}

}
