package fi.benjami.code4jvm.lua.ir.stmt;

import java.util.Arrays;
import java.util.List;

import fi.benjami.code4jvm.Condition;
import fi.benjami.code4jvm.Constant;
import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.Value;
import fi.benjami.code4jvm.block.Block;
import fi.benjami.code4jvm.call.CallTarget;
import fi.benjami.code4jvm.lua.compiler.LoopRef;
import fi.benjami.code4jvm.lua.compiler.LuaContext;
import fi.benjami.code4jvm.lua.ir.IrNode;
import fi.benjami.code4jvm.lua.ir.LuaBlock;
import fi.benjami.code4jvm.lua.ir.LuaLocalVar;
import fi.benjami.code4jvm.lua.ir.LuaType;
import fi.benjami.code4jvm.lua.linker.CallSiteOptions;
import fi.benjami.code4jvm.lua.linker.LuaLinker;
import fi.benjami.code4jvm.lua.runtime.MultiVals;
import fi.benjami.code4jvm.lua.stdlib.LuaException;
import fi.benjami.code4jvm.statement.ArrayAccess;
import fi.benjami.code4jvm.statement.Jump;

public record IteratorForStmt(
		LuaBlock body,
		LoopRef ref,
		List<LuaLocalVar> loopVars,
		List<IrNode> iterable
) implements IrNode {

	@Override
	public Value emit(LuaContext ctx, Block block) {
		// TODO code4jvm's LoopBlock is dangerously useless; return here if/when it is fixed
		var loop = Block.create("iterator for loop");

		// Figure out the JVM variables for loop Lua variables
		var loopJvmVars = loopVars.stream()
				.map(ctx::resolveLocalVar)
				.toList();
		
		var init = Block.create("iterator for init");
		Value next, state;
		var control = loopJvmVars.get(0);
		if (iterable.size() == 1) {
			// Before loop body, call the iterable to (hopefully) produce an array of:
			// iterator function, state, initial value for control variable, (TODO closing value)
			// TODO Java iterable interop?
			ctx.setAllowSpread(true);
			var iterator = iterable.get(0).emit(ctx, init);
			ctx.setAllowSpread(false);
			// FIXME guard against too short array!
			next = init.add(ArrayAccess.get(iterator, Constant.of(0)));
			state = init.add(ArrayAccess.get(iterator, Constant.of(1)));
			init.add(control, ArrayAccess.get(iterator, Constant.of(2)));
		} else {
			// Of course, it doesn't strictly NEED to be one function...
			if (iterable.size() > 0) {
				next = iterable.get(0).emit(ctx, init);
			} else {
				throw new LuaException("for iterator is nil");
			}
			if (iterable.size() > 1) {
				state = iterable.get(1).emit(ctx, init);
			} else {
				state = Constant.nullValue(Type.OBJECT);
			}
			if (iterable.size() > 2) {
				var value = iterable.get(2).emit(ctx, init);
				init.add(control.set(value.cast(control.type())));
			} else {
				init.add(control.set(Constant.nullValue(Type.OBJECT)));
			}
		}
		block.add(init);
		
		// In loop body, call next(state, control)
		// (types are unknown because we can't yet track them for multivals)
		var bootstrap = LuaLinker.BOOTSTRAP_DYNAMIC;
		var options = new CallSiteOptions(ctx.owner(), new LuaType[] {LuaType.UNKNOWN, LuaType.UNKNOWN}, true, false);
		bootstrap = bootstrap.withCapturedArgs(ctx.addClassData(options));
		var target = CallTarget.dynamic(bootstrap, Type.OBJECT.array(1), "_", Type.OBJECT, Type.OBJECT);
		var results = loop.add(target.call(next, state, control));
		
		// TODO add another path for non-array iterators
		var resultCount = loop.add(ArrayAccess.length(results));
		// Assign whatever was returned to loop variables (use nil/null as missing entries)
		for (var loopVar : loopJvmVars) {
			loop.add(loopVar.set(Constant.nullValue(Type.OBJECT)));
		}
		var assignVars = Block.create("assign loop vars");
		for (var i = 0; i < loopJvmVars.size(); i++) {
			// Skip rest of assignments when we've reached end of next's results
			var idx = Constant.of(i);
			assignVars.add(Jump.to(assignVars, Jump.Target.END, Condition.equal(resultCount, idx)));
			var value = assignVars.add(ArrayAccess.get(results, idx));
			assignVars.add(loopJvmVars.get(i).set(value));
		}
		loop.add(assignVars);
		
		// Finally, if the control (=first of loop variables) is null, end loop
		loop.add(Jump.to(loop, Jump.Target.END, Condition.isNull(control)));
		
		// Tell LoopBreaks how to break out of this loop
		ref.breakLoop = b -> b.add(Jump.to(loop, Jump.Target.END));
		body.emit(ctx, loop);
		ref.breakLoop = null;
		
		loop.add(Jump.to(loop, Jump.Target.START)); // And again!
		
		block.add(loop);
		return null;
	}

	@Override
	public LuaType outputType(LuaContext ctx) {
		for (var loopVar : loopVars) {
			ctx.recordType(loopVar, LuaType.UNKNOWN); // TODO typing this would need multival types
		}
		for (var part : iterable) {
			part.outputType(ctx);
		}
		body.outputType(ctx);
		return LuaType.NIL;
	}
	
	@Override
	public boolean hasReturn() {
		return false; // The loop might run for zero iterations
	}
}
