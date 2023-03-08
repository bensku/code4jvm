package fi.benjami.parserkit.parser.internal;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

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
	
	private static final Type TOKEN = Type.of(Token.class);
	static final Type TOKEN_VIEW = Type.of(TokenizedText.View.class);
	
	private static final CallTarget COPY_VIEW = CallTarget.virtualMethod(TOKEN_VIEW, TOKEN_VIEW, "copy");
	private static final CallTarget ADVANCE_VIEW = CallTarget.virtualMethod(TOKEN_VIEW, Type.VOID, "advance", TOKEN_VIEW);
	private static final CallTarget PEEK_TOKEN = CallTarget.virtualMethod(TOKEN_VIEW, TOKEN, "peek");
	private static final CallTarget POP_TOKEN = CallTarget.virtualMethod(TOKEN_VIEW, TOKEN, "pop");
	
	private static final CallTarget GET_TYPE = CallTarget.virtualMethod(TOKEN, Type.INT, "type");
	private static final CallTarget GET_VALUE = CallTarget.virtualMethod(TOKEN, Type.OBJECT, "value");
	
	static final Type AST_NODE = Type.of(AstNode.class);
		
	private final NodeRegistry nodeRegistry;
	private TokenType[] tokenTypes;
	
	private final ClassDef def;
	private final NodeManager nodeManager;
	
	private final boolean hookSupport;
	
	public ParserGenerator(String className, NodeRegistry nodeRegistry, TokenType[] tokenTypes, boolean hookSupport) {
		this.nodeRegistry = nodeRegistry;
		this.tokenTypes = tokenTypes;
		this.def = ClassDef.create(className, Access.PUBLIC);
		def.interfaces(Type.of(Parser.class));
		this.nodeManager = new NodeManager(def.type());
		this.hookSupport = hookSupport;
	}
	
	public void addRoot(Class<? extends AstNode> nodeType) {
		addAstNode(nodeType);
	}
	
	public byte[] compile() {
		// Add generic parse method
		var method = def.addMethod(AST_NODE, "parse", Access.PUBLIC);
		var nodeType = method.arg(Type.of(Class.class));
		var view = method.arg(TOKEN_VIEW);
		
		// Decide which of the parser implementations we should call
		var roots = new IfBlock();
		for (var entry : nodeManager.astNodeParsers().entrySet()) {
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
			handler.add(success.set(Constant.of(false)));
			
			// Consume one token and check if it is what we expected
			var nextToken = handler.add(POP_TOKEN.call(view));
			handler.add(Jump.to(handler, Jump.Target.END, Condition.isNull(nextToken)));
			handler.add(hookCall(ParserHook.TOKEN, Constant.of(token.type().ordinal()), nextToken));
			
			var ordinal = handler.add(GET_TYPE.call(nextToken));
			var successTest = new IfBlock();
			successTest.branch(Condition.equal(Constant.of(token.type().ordinal()), ordinal), block -> {
				// Token is of correct type, store it as input if needed
				var tokenValue = block.add(GET_VALUE.call(nextToken));
				block.add(results.setResult(token.inputId(), tokenValue));
				block.add(success.set(Constant.of(true))); // Operation was success
			});
			handler.add(successTest);
			
			return handler;
		} else if (input instanceof ChoiceInput choices) {
			var handler = Block.create("choices");
			handler.add(success.set(Constant.of(false)));
			
			var nextToken = handler.add(PEEK_TOKEN.call(view));
			handler.add(Jump.to(handler, Jump.Target.END, Condition.isNull(nextToken)));
			
			var ordinal = handler.add(GET_TYPE.call(nextToken));
			handler.add(hookCall(ParserHook.BEFORE_CHOICES, ordinal, Constant.of(choices.toString())));
			
			var viewCopy = Variable.create(TOKEN_VIEW);
			
			// Shared success handler for all choices (inserted after them)
			var onSuccess = Block.create("choice found");
			// If hooks are enabled, notify it about success with this particular choice
			// AND the entire choice group
			// TODO they broke, but code size is more important right now
//			onSuccess.add(hookCall(ParserHook.CHOICE_AFTER_INPUT, Constant.of(choice.toString()), Constant.of(true)));
//			onSuccess.add(hookCall(ParserHook.AFTER_CHOICES, Constant.of(choices.toString()), Constant.of(true)));
			
			// Advance parent view given to us if parsing succeeded
			onSuccess.add(ADVANCE_VIEW.call(view, viewCopy));
			
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
							var newCopy = block.add(COPY_VIEW.call(view));
							block.add(viewCopy.set(newCopy)); // newCopy is on stack, hopefully
							// Parse the choice
							block.add(addInput(viewCopy, choice, results, success, blocker));
							
							// Short-circuit on success
							block.add(Jump.to(onSuccess, Jump.Target.START, Condition.isTrue(success)));

							block.add(hookCall(ParserHook.CHOICE_AFTER_INPUT, Constant.of(choice.toString()), Constant.of(false)));
						}
					});
				} // else: this node type is not accepted here
			}
			handler.add(tokenTest);
			
			
			// Token was not in predict set or the prediction failed to parse
			handler.add(hookCall(ParserHook.AFTER_CHOICES, Constant.of(choices.toString()), Constant.of(false)));
			handler.add(Jump.to(handler, Jump.Target.END));
			
			// Parsing succeeded
			handler.add(onSuccess);
			
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
			handler.add(success.set(Constant.of(false)));
			
			// Check if this node is known to be ALWAYS blocked here
			// This reduces the code size a bit
			var nodeId = nodeRegistry.getTypeId(childNode.type());
//			if (blocker.isAlwaysBlocked(nodeId)) {
//				return handler;
//			}
			
			// If not, insert a check for blocked node to generated code
			var isBlocked = handler.add(blocker.check(nodeId));
			var blacklistTest = new IfBlock();
			// TODO WHY is this smaller than a direct jump?!?
			blacklistTest.branch(Condition.equal(isBlocked, Constant.of(0L)).not(), block -> {
				block.add(hookCall(ParserHook.BEFORE_CHILD_NODE, Constant.of(Type.of(childNode.type())), Constant.of(true)));
				block.add(Jump.to(handler, Jump.Target.END));
			});
			handler.add(blacklistTest);
			handler.add(hookCall(ParserHook.BEFORE_CHILD_NODE, Constant.of(Type.of(childNode.type())), Constant.of(false)));
			
			// Get or create call target to method
			// We don't care if the method itself exists yet
			var parser = nodeManager.astNodeParser(childNode.type());

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
			handler.add(successTest);
			
			return handler;
		} else if (input instanceof VirtualNodeInput virtualNode) {
			// Unlike AST nodes, we must make sure the parser exists or generate it
			var hasParser = nodeManager.hasParser(virtualNode);
			if (!hasParser) {
				addVirtualNode(virtualNode);
			}
			var parser = nodeManager.virtualNodeParser(virtualNode);
			
			var handler = Block.create("virtual node");
			handler.add(success.set(Constant.of(false)));

			// Call the method
			var node = handler.add(parser.call(view, blocker.mask(), blocker.topNode()));
			
			// Check success (node == null on failure) and store the node
			var successTest = new IfBlock();
			successTest.branch(Condition.isNull(node).not(), block -> {
				block.add(results.setResult(virtualNode.inputId(), node));
				block.add(success.set(Constant.of(true)));
			});
			handler.add(successTest);
			
			return handler;

		} else {
			throw new AssertionError();
		}
	}
	
	private void addAstNode(Class<? extends AstNode> nodeType) {		
		var input = nodeRegistry.getPattern(nodeType);
		
		var target = nodeManager.astNodeParser(nodeType);
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
		
		// Take blocked node mask as argument and add this node to it
		var blocker = new NodeBlocker(method.arg(Type.LONG), null);
		blocker = blocker.add(method.block(), nodeRegistry.getTypeId(nodeType));
		
		// Prepare local variables for results
		var results = newResultRegistry(constructor);
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
	
	private void addVirtualNode(VirtualNodeInput input) {
		var target = nodeManager.virtualNodeParser(input);
		var method = def.addStaticMethod(AST_NODE, target.name(), Access.PRIVATE);
		var view = method.arg(TOKEN_VIEW);
		var success = Variable.create(Type.BOOLEAN);
				
		// Take blocked node mask as argument and add this node to it
		var blocker = new NodeBlocker(method.arg(Type.LONG), method.arg(Type.INT));
		
		// Prepare local variables for results
		var results = new ResultRegistry(List.of(new ResultRegistry.InputArg("_virtualNode", AstNode.class)));
		method.add(results.initResults());
		
		// Handle the root input
		method.add(addInput(view, input.input(), results, success, blocker));
		
		// Create and return AST node if we have no failures
		var successTest = new IfBlock();
		successTest.branch(Condition.isTrue(success), block -> {
			var astNode = results.constructorArgs().get(0);
			block.add(Return.value(astNode));
		});
		successTest.fallback(block -> {
			block.add(Return.value(Constant.nullValue(AST_NODE)));
		});
		method.add(successTest);
	}

	
	private ResultRegistry newResultRegistry(Constructor<?> constructor) {
		var inputArgs = new ArrayList<ResultRegistry.InputArg>();
		
		// Look at the constructor parameters for input annotations
		for (var param : constructor.getParameters()) {
			var childNode = param.getAnnotation(ChildNode.class);
			if (childNode != null) {
				var inputId = childNode.value();
				inputArgs.add(new ResultRegistry.InputArg(inputId, param.getType()));
				continue;
			}
			var tokenValue = param.getAnnotation(TokenValue.class);
			if (tokenValue != null) {
				var inputId = tokenValue.value();
				inputArgs.add(new ResultRegistry.InputArg(inputId, param.getType()));
				continue;
			}
		}
				
		return new ResultRegistry(inputArgs);
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
