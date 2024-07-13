package fi.benjami.code4jvm.lua.ir.stmt;

import java.util.List;

import fi.benjami.code4jvm.Condition;
import fi.benjami.code4jvm.Constant;
import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.Value;
import fi.benjami.code4jvm.Variable;
import fi.benjami.code4jvm.block.Block;
import fi.benjami.code4jvm.call.CallTarget;
import fi.benjami.code4jvm.lua.compiler.LoopRef;
import fi.benjami.code4jvm.lua.compiler.LuaContext;
import fi.benjami.code4jvm.lua.ir.IrNode;
import fi.benjami.code4jvm.lua.ir.LuaBlock;
import fi.benjami.code4jvm.lua.ir.LuaLocalVar;
import fi.benjami.code4jvm.lua.ir.LuaType;
import fi.benjami.code4jvm.lua.ir.expr.FunctionCallExpr;
import fi.benjami.code4jvm.lua.linker.CallSiteOptions;
import fi.benjami.code4jvm.lua.linker.LuaLinker;
import fi.benjami.code4jvm.lua.stdlib.LuaException;
import fi.benjami.code4jvm.statement.ArrayAccess;
import fi.benjami.code4jvm.statement.Instanceof;
import fi.benjami.code4jvm.statement.Jump;
import fi.benjami.code4jvm.structure.IfBlock;

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
		Variable next = Variable.create(Type.OBJECT), state = Variable.create(Type.OBJECT);
		var control = loopJvmVars.get(0);
		if (iterable.size() == 1) {
			// Before loop body, call the iterable to (hopefully) produce an array of:
			// iterator function, state, initial value for control variable, (TODO closing value)
			// TODO Java iterable interop?
			var first = iterable.get(0);
			Value iterator;
			if (first instanceof FunctionCallExpr call) {
				ctx.setAllowSpread(true);
				iterator = call.emit(ctx, block, "iteratorFor");
				ctx.setAllowSpread(false);
			} else {
				iterator = first.emit(ctx, block);
			}
			
			// We might've gotten a multival of next, state, control or only some of those
			// Set state, control to null as they are technically optional
			init.add(state.set(Constant.nullValue(Type.OBJECT)));
			init.add(control.set(Constant.nullValue(Type.OBJECT)));
			
			// Extract the values
			var innerInit = new IfBlock();
			innerInit.branch(inner -> {
				var isArray = inner.add(Instanceof.isInstance(iterator, Type.OBJECT.array(1)));
				return Condition.isTrue(isArray);
			}, inner -> {
				// Multival, extract array elements
				var array = iterator.cast(Type.OBJECT.array(1));
				var length = inner.add(ArrayAccess.length(array));
				inner.add(next, ArrayAccess.get(array, Constant.of(0))); // This must exist, but TODO improve error messages
				inner.add(Jump.to(init, Jump.Target.END, Condition.equal(length, Constant.of(1))));
				inner.add(ArrayAccess.get(array, Constant.of(1)));
				inner.add(Jump.to(init, Jump.Target.END, Condition.equal(length, Constant.of(2))));
				inner.add(control, ArrayAccess.get(array, Constant.of(2)));
			});
			innerInit.fallback(inner -> {
				// Only one value, the iterator function
				inner.add(next.set(iterator));
			});
			init.add(innerInit);
		} else {
			// Of course, it doesn't strictly NEED to be one function...
			if (iterable.size() > 0) {
				init.add(next.set(iterable.get(0).emit(ctx, init)));
			} else {
				throw new LuaException("for iterator is nil");
			}
			if (iterable.size() > 1) {
				init.add(state.set(iterable.get(1).emit(ctx, init)));
			} else {
				init.add(state.set(Constant.nullValue(Type.OBJECT)));
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
		var bootstrap = LuaLinker.BOOTSTRAP_DYNAMIC;
		// Types are unknown because we can't yet track them for multivals
		var options = new CallSiteOptions(ctx.owner(), new LuaType[] {LuaType.UNKNOWN, LuaType.UNKNOWN}, true, false);
		bootstrap = bootstrap.withCapturedArgs(ctx.addClassData(options));
		var target = CallTarget.dynamic(bootstrap, Type.OBJECT, "_", Type.OBJECT, Type.OBJECT);
		var results = loop.add(target.call(next, state, control));
		
		// TODO add another path for non-array iterators
		// Initialize loop variables with nulls, since some might not be assigned anything
		for (var loopVar : loopJvmVars) {
			loop.add(loopVar.set(Constant.nullValue(Type.OBJECT)));
		}
		
		// Assign whatever was returned to loop variables
		var assignVars = new IfBlock();
		assignVars.branch(inner -> {
			var isArray = inner.add(Instanceof.isInstance(results, Type.OBJECT.array(1)));
			return Condition.isTrue(isArray);
		}, inner -> {
			// Got multival for loop variables
			var array = results.cast(Type.OBJECT.array(1));
			var resultCount = inner.add(ArrayAccess.length(array));
			for (var i = 0; i < loopJvmVars.size(); i++) {
				// Skip rest of assignments when we've reached end of next's results
				var idx = Constant.of(i);
				inner.add(Jump.to(inner, Jump.Target.END, Condition.equal(resultCount, idx)));
				var value = inner.add(ArrayAccess.get(array, idx));
				inner.add(loopJvmVars.get(i).set(value));
			}
		});
		assignVars.fallback(inner -> {
			// Only one value, assign it to the first loop var (control)
			inner.add(control.set(results));
		});
		
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
