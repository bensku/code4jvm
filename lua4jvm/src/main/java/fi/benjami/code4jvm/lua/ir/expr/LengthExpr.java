package fi.benjami.code4jvm.lua.ir.expr;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import fi.benjami.code4jvm.Value;
import fi.benjami.code4jvm.block.Block;
import fi.benjami.code4jvm.lua.compiler.LuaContext;
import fi.benjami.code4jvm.lua.ir.IrNode;
import fi.benjami.code4jvm.lua.ir.LuaType;
import fi.benjami.code4jvm.lua.linker.CallSiteOptions;
import fi.benjami.code4jvm.lua.linker.DynamicTarget;
import fi.benjami.code4jvm.lua.linker.LuaLinker;
import fi.benjami.code4jvm.lua.linker.UnaryOp;
import fi.benjami.code4jvm.lua.runtime.LuaTable;
import fi.benjami.code4jvm.lua.stdlib.LuaException;
import fi.benjami.code4jvm.statement.Arithmetic;

public record LengthExpr(IrNode expr) implements IrNode {

	private static final MethodHandle TABLE_LENGTH, STRING_LENGTH;
	private static final DynamicTarget TARGET;
	
	static {
		var lookup = MethodHandles.lookup();
		try {
			TABLE_LENGTH = MethodHandles.dropArguments(lookup.findVirtual(LuaTable.class, "arraySize", MethodType.methodType(int.class))
					.asType(MethodType.methodType(double.class, LuaTable.class)), 0, Object.class);
			STRING_LENGTH = MethodHandles.dropArguments(lookup.findVirtual(String.class, "length", MethodType.methodType(int.class))
					.asType(MethodType.methodType(double.class, String.class)), 0, Object.class);
		} catch (NoSuchMethodException | IllegalAccessException e) {
			throw new AssertionError(e);
		}
		
		TARGET = UnaryOp.newTarget(new UnaryOp.Path[] {
				new UnaryOp.Path(String.class, STRING_LENGTH),
					new UnaryOp.Path(LuaTable.class, TABLE_LENGTH)
				}, "__len",
				(val) -> new LuaException("attempted to get length of non-string or table value"));
	}
	
	@Override
	public Value emit(LuaContext ctx, Block block) {
		// TODO setup direct calls if static analysis has enough information?
		var value = expr.emit(ctx, block);
		return block.add(LuaLinker.setupCall(ctx, CallSiteOptions.nonFunction(ctx.owner(), LuaType.UNKNOWN, LuaType.UNKNOWN), TARGET, value));
	}

	@Override
	public LuaType outputType(LuaContext ctx) {
		// We can't do type analysis through metatables (yet)
		return expr.outputType(ctx).equals(LuaType.STRING) ? LuaType.NUMBER : LuaType.UNKNOWN;
	}

}
