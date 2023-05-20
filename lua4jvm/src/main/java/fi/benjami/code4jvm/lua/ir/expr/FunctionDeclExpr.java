package fi.benjami.code4jvm.lua.ir.expr;

import java.util.Arrays;

import fi.benjami.code4jvm.Constant;
import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.Value;
import fi.benjami.code4jvm.block.Block;
import fi.benjami.code4jvm.lua.compiler.LuaContext;
import fi.benjami.code4jvm.lua.ir.IrNode;
import fi.benjami.code4jvm.lua.ir.LuaBlock;
import fi.benjami.code4jvm.lua.ir.LuaLocalVar;
import fi.benjami.code4jvm.lua.ir.LuaType;
import fi.benjami.code4jvm.lua.ir.UpvalueTemplate;
import fi.benjami.code4jvm.lua.runtime.LuaFunction;
import fi.benjami.code4jvm.statement.ArrayAccess;

public record FunctionDeclExpr(
		Upvalue[] upvalues,
		
		/**
		 * Arguments inside the new function.
		 */
		LuaLocalVar[] arguments,
		
		/**
		 * Function body.
		 */
		LuaBlock body
) implements IrNode {
	
	public record Upvalue(
			LuaLocalVar inside,
			LuaLocalVar outside
	) {}

	@Override
	public Value emit(LuaContext ctx, Block block) {
		// Generate code only for creating LuaFunction
		// The function is not compiled until it is called!
		
		// Copy local variables to upvalues array
		var upvalueValues = block.add(Type.OBJECT.array(1).newInstance(Constant.of(upvalues.length)));
		for (var i = 0; i < upvalues.length; i++) {
			var value = ctx.resolveLocalVar(upvalues[i].outside());
			block.add(ArrayAccess.set(upvalueValues, Constant.of(i), value.cast(Type.OBJECT)));
		}
		
		var type = outputType(ctx);
		return block.add(LuaFunction.TYPE.newInstance(ctx.addClassData(type), upvalueValues));
	}

	@Override
	public LuaType.Function outputType(LuaContext ctx) {
		// Upvalue template has the variable INSIDE declared function, with type of OUTSIDE variable
		var upvalueTemplates = Arrays.stream(upvalues)
				.map(upvalue -> new UpvalueTemplate(upvalue.inside(), ctx.variableType(upvalue.outside())))
				.toArray(UpvalueTemplate[]::new);
		return LuaType.function(upvalueTemplates, arguments, body);
	}

}
