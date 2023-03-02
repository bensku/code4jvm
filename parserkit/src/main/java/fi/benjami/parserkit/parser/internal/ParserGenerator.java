package fi.benjami.parserkit.parser.internal;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fi.benjami.code4jvm.Condition;
import fi.benjami.code4jvm.Constant;
import fi.benjami.code4jvm.Statement;
import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.Value;
import fi.benjami.code4jvm.Variable;
import fi.benjami.code4jvm.block.Block;
import fi.benjami.code4jvm.call.CallTarget;
import fi.benjami.code4jvm.flag.Access;
import fi.benjami.code4jvm.statement.Jump;
import fi.benjami.code4jvm.statement.NoOp;
import fi.benjami.code4jvm.statement.Return;
import fi.benjami.code4jvm.structure.IfBlock;
import fi.benjami.code4jvm.structure.LoopBlock;
import fi.benjami.code4jvm.typedef.ClassDef;
import fi.benjami.parserkit.lexer.Token;
import fi.benjami.parserkit.lexer.TokenType;
import fi.benjami.parserkit.lexer.TokenizedText;
import fi.benjami.parserkit.parser.Input;
import fi.benjami.parserkit.parser.NodeRegistry;
import fi.benjami.parserkit.parser.Parser;
import fi.benjami.parserkit.parser.ast.AstNode;
import fi.benjami.parserkit.parser.ast.ChildNode;
import fi.benjami.parserkit.parser.ast.NodeCreator;
import fi.benjami.parserkit.parser.ast.TokenValue;

public class ParserGenerator {
	
	private static final Type LIST = Type.of(List.class);
	private static final Type ARRAY_LIST = Type.of(ArrayList.class);
	private static final Type TOKEN = Type.of(Token.class);
	private static final Type TOKEN_VIEW = Type.of(TokenizedText.View.class);

	private static final CallTarget LIST_ADD = CallTarget.virtualMethod(LIST, Type.BOOLEAN, "add", Type.OBJECT);
	
	private static final CallTarget COPY_VIEW = CallTarget.virtualMethod(TOKEN_VIEW, TOKEN_VIEW, "copy");
	private static final CallTarget ADVANCE_VIEW = CallTarget.virtualMethod(TOKEN_VIEW, Type.VOID, "advance", TOKEN_VIEW);
	private static final CallTarget PEEK_TOKEN = CallTarget.virtualMethod(TOKEN_VIEW, TOKEN, "peek");
	private static final CallTarget POP_TOKEN = CallTarget.virtualMethod(TOKEN_VIEW, TOKEN, "pop");
	
	private static final CallTarget GET_TYPE = CallTarget.virtualMethod(TOKEN, Type.INT, "type");
	private static final CallTarget GET_VALUE = CallTarget.virtualMethod(TOKEN, Type.OBJECT, "value");
	
	private static final Type AST_NODE = Type.of(AstNode.class);
	
	private final NodeRegistry nodeRegistry;
	private TokenType[] tokenTypes;
	
	private final Map<Class<? extends AstNode>, CallTarget> nodeParsers;
	private final ClassDef def;
	
	private final boolean hookSupport;
	
	public ParserGenerator(String className, NodeRegistry nodeRegistry, TokenType[] tokenTypes, boolean hookSupport) {
		this.nodeRegistry = nodeRegistry;
		this.tokenTypes = tokenTypes;
		this.nodeParsers = new HashMap<>();
		this.def = ClassDef.create(className, Access.PUBLIC);
		def.interfaces(Type.of(Parser.class));
		this.hookSupport = hookSupport;
	}
	
	public void addRoot(Class<? extends AstNode> nodeType) {
		addMethod(nodeType);
	}
	
	public byte[] compile() {
		// Add generic parse method
		var method = def.addMethod(AST_NODE, "parse", Access.PUBLIC);
		var nodeType = method.arg(Type.of(Class.class));
		var view = method.arg(TOKEN_VIEW);
		
		// Decide which of the parser implementations we should call
		var roots = new IfBlock();
		for (var entry : nodeParsers.entrySet()) {
			roots.branch(Condition.equal(Constant.of(Type.of(entry.getKey())), nodeType), block -> {
				// Begin parsing with empty blacklist
				var node = block.add(entry.getValue().call(view, Constant.of(0L)));
				block.add(Return.value(node));
			});
		}
		method.add(roots);
		method.add(Return.value(Constant.nullValue(AST_NODE)));
		
		// Add default constructor
		def.addEmptyConstructor(Access.PUBLIC);
		
		if (hookSupport) {
			def.addStaticField(Access.PUBLIC, Type.of(ParserHook.class), "HOOK");
		}
		
		return def.compile();
	}
	
	private Block addInput(Value view, Input input, ResultRegistry results,
			Variable success, NodeBlocker blocker) {
		
		if (input instanceof TokenInput token) {
			var handler = Block.create("token " + token.type());
			
			// Consume one token and check if it is what we expected
			var nextToken = handler.add(POP_TOKEN.call(view));
			var nullTest = new IfBlock();
			nullTest.branch(Condition.isNull(nextToken), block -> {
				block.add(success.set(Constant.of(false)));
				block.add(Jump.to(handler, Jump.Target.END));
			});
			handler.add(nullTest);
			handler.add(hookCall(ParserHook.TOKEN, Constant.of(token.type().ordinal()), nextToken));
			
			var ordinal = handler.add(GET_TYPE.call(nextToken));
			var successTest = new IfBlock();
			successTest.branch(Condition.equal(Constant.of(token.type().ordinal()), ordinal), block -> {
				// Token is of correct type, store it as input if needed
				var tokenValue = block.add(GET_VALUE.call(nextToken));
				block.add(results.setResult(token.inputId(), tokenValue));
				block.add(success.set(Constant.of(true))); // Operation was success
			});
			successTest.fallback(block -> {
				block.add(success.set(Constant.of(false)));
			});
			handler.add(successTest);
			
			return handler;
		} else if (input instanceof ChoiceInput choices) {
			var handler = Block.create("choices");
			
			var nextToken = handler.add(PEEK_TOKEN.call(view));
			var nullTest = new IfBlock();
			nullTest.branch(Condition.isNull(nextToken), block -> {
				block.add(success.set(Constant.of(false)));
				block.add(Jump.to(handler, Jump.Target.END));
			});
			handler.add(nullTest);
			
			var ordinal = handler.add(GET_TYPE.call(nextToken));
			handler.add(hookCall(ParserHook.BEFORE_CHOICES, ordinal, Constant.of(choices.toString())));
			
			// Try to select choice using the predict set
			// TODO use tableswitch for large tables?
			// TODO currently everything predict set generates a lot of duplicate code
			// (but this is difficult to solve without breaking input priorities)
			var tokenTest = new IfBlock();
			for (var type : tokenTypes) {
				var validChoices = choices.filterFor(nodeRegistry, type);
				if (!validChoices.isEmpty()) {
					tokenTest.branch(Condition.equal(Constant.of(type.ordinal()), ordinal), block -> {
						block.add(hookCall(ParserHook.CHOICE_TOKEN, Constant.of(type.ordinal()), Constant.of(validChoices.toString())));
						for (var choice : validChoices) {
							block.add(hookCall(ParserHook.CHOICE_BEFORE_INPUT, Constant.of(choice.toString())));
							
							// Take a copy of view, backtracing may be needed when there are multiple choices
							var viewCopy = block.add(COPY_VIEW.call(view));
							// Parse the choice
							block.add(addInput(viewCopy, choice, results, success, blocker));
							
							var successTest = new IfBlock();
							successTest.branch(Condition.isTrue(success), inner -> {
								// If hooks are enabled, notify it about success with this particular choice
								// AND the entire choice group
								inner.add(hookCall(ParserHook.CHOICE_AFTER_INPUT, Constant.of(choice.toString()), Constant.of(true)));
								inner.add(hookCall(ParserHook.AFTER_CHOICES, Constant.of(choices.toString()), Constant.of(true)));
								
								// Advance parent view given to us if parsing succeeded
								inner.add(ADVANCE_VIEW.call(view, viewCopy));
								
								// Short-circuit on success by jumping to end
								inner.add(Jump.to(handler, Jump.Target.END));
							}); // On failure, continue to next choice
							block.add(successTest);
							block.add(hookCall(ParserHook.CHOICE_AFTER_INPUT, Constant.of(choice.toString()), Constant.of(false)));
						}
					});
				} // else: this node type is not accepted here
			}
			handler.add(tokenTest);
			
			// Token was not in predict set or the prediction failed to parse
			handler.add(success.set(Constant.of(false)));
			handler.add(hookCall(ParserHook.AFTER_CHOICES, Constant.of(choices.toString()), Constant.of(false)));
			
			return handler;
		} else if (input instanceof CompoundInput compound) {
			var handler = Block.create("compound");
			handler.add(hookCall(ParserHook.BEFORE_COMPOUND, Constant.of(compound.toString())));

			// Compound inputs use two sets of blocked node masks
			// The first one includes everything - parent nodes, current node
			// The second one includes parent nodes but NOT the current one
			// This is needed to allow right recursion
			// If parents were not blocked, we'd likely misparse nested expressions
			var currentBlocker = blocker;
			var rightBlocker = blocker.pop(handler);
			
			var viewCopy = handler.add(COPY_VIEW.call(view));
			var parts = compound.parts();
			for (var i = 0; i < parts.size(); i++) {
				var part = parts.get(i);
				handler.add(hookCall(ParserHook.COMPOUND_BEFORE_PART, Constant.of(part.toString()), Constant.of(i)));
				
				handler.add(addInput(viewCopy, part, results, success, currentBlocker));
				// Jump to end on failure (short-circuit)
				handler.add(Jump.to(handler, Jump.Target.END, Condition.isFalse(success)));
				
				// TODO what about failure hook?
				handler.add(hookCall(ParserHook.COMPOUND_AFTER_PART, Constant.of(part.toString()), Constant.of(i), Constant.of(true)));
				
				// Allow right recursion
				currentBlocker = rightBlocker;
			}
			// If all parts were found, advance the parent view
			handler.add(ADVANCE_VIEW.call(view, viewCopy));
			
			return handler;
		} else if (input instanceof RepeatingInput repeating) {
			var handler = Block.create("repeating");
			
			// TODO callback-based API for LoopBlock?
			var viewCopy = handler.add(COPY_VIEW.call(view));
			var body = Block.create("repeating");
			var loop = LoopBlock.whileLoop(body, Condition.always(true));
			body.add(addInput(viewCopy, repeating.input(), results, success, blocker));
			
			var successTest = new IfBlock();
			successTest.branch(Condition.isTrue(success), block -> {
				// Advance view given to us if parsing succeeded
				block.add(ADVANCE_VIEW.call(view, viewCopy));
			});
			successTest.fallback(block -> {
				// Repeating zero times is perfectly ok -> success
				block.add(success.set(Constant.of(true)));
				block.add(loop.breakStmt());
			});
			body.add(successTest);
			
			handler.add(loop);
			
			return handler;
		} else if (input instanceof ChildNodeInput childNode) {
			var handler = Block.create("node " + childNode.type());
			
			// If this node is blocked, refuse to parse it
			var isBlocked = handler.add(blocker.check(nodeRegistry.getTypeId(childNode.type())));
			var blacklistTest = new IfBlock();
			blacklistTest.branch(Condition.equal(isBlocked, Constant.of(0L)).not(), block -> {
				block.add(hookCall(ParserHook.BEFORE_CHILD_NODE, Constant.of(Type.of(childNode.type())), Constant.of(true)));
				block.add(success.set(Constant.of(false)));
				block.add(Jump.to(handler, Jump.Target.END));
			});
			handler.add(blacklistTest);
			handler.add(hookCall(ParserHook.BEFORE_CHILD_NODE, Constant.of(Type.of(childNode.type())), Constant.of(false)));
			
			// Get or create call target to method
			// We don't care if the method itself exists yet
			var parser = getParserMethod(childNode.type());

			// Call the method
			var viewCopy = handler.add(COPY_VIEW.call(view));
			var node = handler.add(parser.call(viewCopy, blocker.mask()));
			
			// Check success (node == null on failure) and store the node
			var successTest = new IfBlock();
			successTest.branch(Condition.isNull(node).not(), block -> {
				block.add(ADVANCE_VIEW.call(view, viewCopy));
				block.add(results.setResult(childNode.inputId(), node));
				block.add(success.set(Constant.of(true)));
			});
			successTest.fallback(block -> {
				block.add(success.set(Constant.of(false)));
			});
			handler.add(successTest);
			
			return handler;
		} else {
			throw new AssertionError();
		}
	}
	
	private CallTarget getParserMethod(Class<? extends AstNode> type) {
		return nodeParsers.computeIfAbsent(type, k -> {
			var name = "parser$" + type.getSimpleName();
			// TODO > 64 nodes blacklist support
			return def.type().staticMethod(AST_NODE, name, TOKEN_VIEW, Type.LONG);
		});
	}
	
	private void addMethod(Class<? extends AstNode> nodeType) {		
		var input = nodeRegistry.getPattern(nodeType);
		
		var target = getParserMethod(nodeType);
		var method = def.addStaticMethod(AST_NODE, target.name(), Access.PRIVATE);
		var view = method.arg(TOKEN_VIEW);
		var success = Variable.create(Type.BOOLEAN);
		
		// Find constructor for the AST node
		Constructor<?> constructor = null;
		for (var option : nodeType.getConstructors()) {
			if (option.isAnnotationPresent(NodeCreator.class)) {
				constructor = option;
				break;
			}
		}
		if (constructor == null) {
			if (nodeType.isRecord()) {
				// Default to first (=canonical) constructor of the record
				constructor = nodeType.getConstructors()[0];
			} else {
				throw new AssertionError(); // TODO error handling
			}
		}
		var inputArgs = findInputs(constructor);
		
		// Take blocked node mask as argument and add this node to it
		var blocker = new NodeBlocker(method.arg(Type.LONG));
		blocker = blocker.add(method.block(), nodeRegistry.getTypeId(nodeType));
		
		// Prepare local variables for results
		var results = new ResultRegistry(inputArgs);
		method.add(results.initResults());
		
		// Handle the root input
		method.add(addInput(view, input, results, success, blocker));
		
		// Create and return AST node if we have no failures
		var successTest = new IfBlock();
		successTest.branch(Condition.isTrue(success), block -> {
			var astNode = block.add(Type.of(nodeType).newInstance(results.constructorArgs().toArray(Value[]::new)));
			block.add(Return.value(astNode.asType(AST_NODE)));
		});
		successTest.fallback(block -> {
			block.add(Return.value(Constant.nullValue(AST_NODE)));
		});
		method.add(successTest);
	}
	
	private record InputArg(String inputId, Class<?> type) {}
	
	private List<InputArg> findInputs(Constructor<?> constructor) {
		var inputArgs = new ArrayList<InputArg>();
		
		// Look at the constructor parameters for input annotations
		for (var param : constructor.getParameters()) {
			var childNode = param.getAnnotation(ChildNode.class);
			if (childNode != null) {
				var inputId = childNode.value();
				inputArgs.add(new InputArg(inputId, param.getType()));
				continue;
			}
			var tokenValue = param.getAnnotation(TokenValue.class);
			if (tokenValue != null) {
				var inputId = tokenValue.value();
				inputArgs.add(new InputArg(inputId, param.getType()));
				continue;
			}
		}
				
		return inputArgs;
	}
	
	private static class ResultRegistry {
		
		private final Map<String, Variable> inputMap;
		private final List<Variable> inputList;
		
		public ResultRegistry(List<InputArg> inputArgs) {
			this.inputMap = new HashMap<>();
			this.inputList = new ArrayList<>();
			for (var arg : inputArgs) {
				var variable = Variable.create(Type.of(arg.type()));
				inputMap.put(arg.inputId(), variable);
				inputList.add(variable);
			}
		}
		
		public Statement initResults() {
			return block -> {				
				for (var variable : inputMap.values() ) {
					if (variable.type().equals(LIST)) {
						var list = block.add(ARRAY_LIST.newInstance());
						block.add(variable.set(list.asType(LIST)));
					} else {						
						block.add(variable.set(Constant.nullValue(variable.type())));
					}
				}
			};
		}
		
		public Statement setResult(String inputId, Value value) {
			var target = inputMap.get(inputId);
			if (target == null) {
				return NoOp.INSTANCE;
			} else {
				if (target.type().equals(LIST)) {
					return LIST_ADD.call(target, value);
				} else {					
					return target.set(value.cast(target.type()));
				}
			}
		}
		
		public List<Variable> constructorArgs() {
			return inputList;
		}
	}
	
	private Statement hookCall(CallTarget target, Value... args) {
		if (hookSupport) {
			return block -> {				
				var hook = block.add(def.type().getStatic(Type.of(ParserHook.class), "HOOK"));
				block.add(target.withCapturedArgs(hook).call(args));
			};
		} else {
			return NoOp.INSTANCE;
		}
	}

}
