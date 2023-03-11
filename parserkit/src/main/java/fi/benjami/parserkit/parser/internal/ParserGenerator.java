package fi.benjami.parserkit.parser.internal;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import fi.benjami.code4jvm.Condition;
import fi.benjami.code4jvm.Constant;
import fi.benjami.code4jvm.Statement;
import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.Value;
import fi.benjami.code4jvm.Variable;
import fi.benjami.code4jvm.block.Block;
import fi.benjami.code4jvm.call.CallTarget;
import fi.benjami.code4jvm.flag.Access;
import fi.benjami.code4jvm.statement.BitOp;
import fi.benjami.code4jvm.statement.Jump;
import fi.benjami.code4jvm.statement.NoOp;
import fi.benjami.code4jvm.statement.Return;
import fi.benjami.code4jvm.structure.IfBlock;
import fi.benjami.code4jvm.structure.LoopBlock;
import fi.benjami.code4jvm.typedef.ClassDef;
import fi.benjami.parserkit.lexer.Token;
import fi.benjami.parserkit.lexer.TokenType;
import fi.benjami.parserkit.lexer.TokenizedText;
import fi.benjami.parserkit.parser.CompileError;
import fi.benjami.parserkit.parser.Input;
import fi.benjami.parserkit.parser.NodeRegistry;
import fi.benjami.parserkit.parser.ParseResult;
import fi.benjami.parserkit.parser.Parser;
import fi.benjami.parserkit.parser.ast.AstNode;
import fi.benjami.parserkit.parser.ast.ChildNode;
import fi.benjami.parserkit.parser.ast.NodeCreator;
import fi.benjami.parserkit.parser.ast.TokenValue;
import fi.benjami.parserkit.parser.internal.input.ChildNodeInput;
import fi.benjami.parserkit.parser.internal.input.ChoiceInput;
import fi.benjami.parserkit.parser.internal.input.CompoundInput;
import fi.benjami.parserkit.parser.internal.input.WrapperInput;
import fi.benjami.parserkit.parser.internal.input.RepeatingInput;
import fi.benjami.parserkit.parser.internal.input.TokenInput;
import fi.benjami.parserkit.parser.internal.input.VirtualNodeInput;

public class ParserGenerator {
	
	static final Type ARRAY_LIST = Type.of(ArrayList.class);
	static final Type LIST = Type.of(List.class);
	static final CallTarget LIST_ADD = CallTarget.virtualMethod(LIST, Type.BOOLEAN, "add", Type.OBJECT);
	
	static final Type HASH_SET = Type.of(HashSet.class);
	static final Type SET = Type.of(Set.class);
	static final CallTarget SET_ADD = CallTarget.virtualMethod(Type.of(Set.class), Type.BOOLEAN, "add", Type.OBJECT);
	
	static final Type TOKEN = Type.of(Token.class);
	static final Type TOKEN_VIEW = Type.of(TokenizedText.View.class);
	
	private static final CallTarget COPY_VIEW = CallTarget.virtualMethod(TOKEN_VIEW, TOKEN_VIEW, "copy");
	private static final CallTarget ADVANCE_VIEW = CallTarget.virtualMethod(TOKEN_VIEW, Type.VOID, "advance", TOKEN_VIEW);
	
	private static final CallTarget GET_TYPE = CallTarget.virtualMethod(TOKEN, Type.INT, "type");
	private static final CallTarget GET_VALUE = CallTarget.virtualMethod(TOKEN, Type.OBJECT, "value");
	
	static final Type AST_NODE = Type.of(AstNode.class);
	private static final Type PARSE_RESULT = Type.of(ParseResult.class);
		
	private final NodeRegistry nodeRegistry;
	private TokenType[] tokenTypes;
	
	private final ClassDef def;
	private final NodeManager nodeManager;
	
	private final boolean hookSupport;
	
	private final CallTarget peekToken;
	private final CallTarget popToken;
	
	public ParserGenerator(String className, NodeRegistry nodeRegistry, TokenType[] tokenTypes, boolean hookSupport) {
		this.nodeRegistry = nodeRegistry;
		this.tokenTypes = tokenTypes;
		this.def = ClassDef.create(className, Access.PUBLIC);
		def.interfaces(Type.of(Parser.class));
		this.nodeManager = new NodeManager(def.type());
		this.hookSupport = hookSupport;
		
		var visibleMask = visibleTokenMask(tokenTypes);
		var errorMask = errorTokenMask(tokenTypes);
		this.peekToken = addNextTokenHelper(visibleMask, errorMask, "$peek", "peek");
		this.popToken = addNextTokenHelper(visibleMask, errorMask, "$pop", "pop");
	}
	
	private static Constant visibleTokenMask(TokenType[] tokenTypes) {
		var mask = 0L;
		for (TokenType type : tokenTypes) {
			if ((type.flags() & TokenType.FLAG_INVISIBLE) == 0) {
				mask |= 1L << type.ordinal();
			}
		}
		return Constant.of(mask);
	}
	
	private static Constant errorTokenMask(TokenType[] tokenTypes) {
		var mask = 0L;
		for (TokenType type : tokenTypes) {
			if ((type.flags() & TokenType.FLAG_ERROR) != 0) {
				mask |= 1L << type.ordinal();
			}
		}
		return Constant.of(mask);
	}
	
	public void addRoot(Class<? extends AstNode> nodeType) {
		addAstNode(nodeType);
	}
	
	public byte[] compile() {
		addPublicParseMethod();
		
		// Add default constructor
		def.addEmptyConstructor(Access.PUBLIC);
		
		if (hookSupport) {
			def.addStaticField(Access.PUBLIC, Type.of(ParserHook.class), "HOOK");
		}
		
		return def.compile();
	}
	
	private void addPublicParseMethod() {
		var method = def.addMethod(PARSE_RESULT, "parse", Access.PUBLIC);
		var nodeType = method.arg(Type.of(Class.class));
		var view = method.arg(TOKEN_VIEW);
		var errorSet = method.add(HASH_SET.newInstance()).asType(SET);
		
		var node = Variable.create(AST_NODE);
		method.add(node.set(Constant.nullValue(AST_NODE)));
		
		// Decide which of the parser implementations we should call
		var roots = new IfBlock();
		for (var entry : nodeManager.astNodeParsers().entrySet()) {
			roots.branch(Condition.equal(Constant.of(Type.of(entry.getKey())), nodeType), block -> {
				// Begin parsing with empty blacklist
				block.add(node.set(block.add(entry.getValue().call(view, Constant.of(0L), errorSet))));
			});
		}
		method.add(roots);
		
		var result = method.add(PARSE_RESULT.newInstance(node, errorSet));
		method.add(Return.value(result));
	}
	
	private CallTarget addNextTokenHelper(Value visibleTokenMask, Value errorTokenMask, String name, String getter) {
		var method = def.addStaticMethod(TOKEN, name, Access.PRIVATE);
		var view = method.arg(TOKEN_VIEW);
		var errors = new ErrorManager(method.arg(SET));
		
		var token = Variable.create(TOKEN);
		method.add(token.set(method.add(view.callVirtual(TOKEN, getter))));
		
		var tokenFound = Block.create();
		tokenFound.add(Return.value(token));
		
		var loop = Block.create();
		
		// If token is null, return it (generated parser should handle that)
		loop.add(Jump.to(tokenFound, Jump.Target.START, Condition.isNull(token)));
		
		// If token is visible, return it
		var type = loop.add(GET_TYPE.call(token));
		var mask = loop.add(BitOp.shiftLeft(Constant.of(1L), type));
		var isVisible = loop.add(BitOp.and(mask, visibleTokenMask));
		// FIXME code4jvm: promote RHS to long automatically or throw, don't miscompile it!
		loop.add(Jump.to(tokenFound, Jump.Target.START, Condition.equal(isVisible, Constant.of(0L)).not()));
		
		// Check if the current token is error
		var isError = loop.add(BitOp.and(mask, errorTokenMask));
		var prevToken = loop.add(token.copy());
		
		// No matter the result, take the next token
		// Yes, we'll pop() invisible tokens even when peeking; that is (hopefully) safe
		loop.add(token.set(loop.add(view.callVirtual(TOKEN, "pop"))));
		
		// If the token is not error, jump to loop start
		loop.add(Jump.to(loop, Jump.Target.START, Condition.equal(isError, Constant.of(0L))));
		
		// If it is error, record it and jump to start
		loop.add(errors.errorAtToken(CompileError.LEXICAL, prevToken));
		loop.add(Jump.to(loop, Jump.Target.START));
		
		method.add(loop);
		method.add(tokenFound);
		
		return CallTarget.staticMethod(def.type(), TOKEN, name, TOKEN_VIEW, SET);
	}
	
	private Block addInput(Value view, Input input, ResultRegistry results, ErrorManager errors,
			Variable success, NodeBlocker blocker) {
		if (input instanceof TokenInput token) {
			var handler = Block.create("token " + token.type());
			handler.add(success.set(Constant.of(false)));
			
			// Consume one token and check if it is what we expected
			var nextToken = handler.add(popToken.call(view, errors.errorSet()));
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
			
			var viewCopy = Variable.create(TOKEN_VIEW);
			
			// Shared success handler for all choices (inserted after them)
			var onSuccess = Block.create("choice found");
			// If hooks are enabled, notify it about success with this particular choice
			// AND the entire choice group
			// TODO they broke, but code size is more important right now
			onSuccess.add(hookCall(ParserHook.CHOICE_AFTER_INPUT, Constant.of("UNKNOWN"), Constant.of(true)));
			onSuccess.add(hookCall(ParserHook.AFTER_CHOICES, Constant.of(choices.toString()), Constant.of(true)));
			
			// Advance parent view given to us if parsing succeeded
			onSuccess.add(ADVANCE_VIEW.call(view, viewCopy));
			
			// Fallback handler
			var fallback = Block.create("fallback");
			fallback.add(hookCall(ParserHook.AFTER_CHOICES, Constant.of(choices.toString()), Constant.of(false)));
			if (choices.fallback() != null) {
				var newCopy = fallback.add(COPY_VIEW.call(view));
				fallback.add(viewCopy.set(newCopy));
				fallback.add(addInput(viewCopy, choices.fallback(), results, errors, success, blocker));
				fallback.add(Jump.to(handler, Jump.Target.END, Condition.isFalse(success)));
			} else {
				fallback.add(Jump.to(handler, Jump.Target.END));
			}
			
			// Peek at the next token
			var nextToken = handler.add(peekToken.call(view, errors.errorSet()));
			// If it is null, jump directly to fallback for error handling
			handler.add(Jump.to(fallback, Jump.Target.START, Condition.isNull(nextToken)));
			
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
							var newCopy = block.add(COPY_VIEW.call(view));
							block.add(viewCopy.set(newCopy)); // newCopy is on stack, hopefully
							// Parse the choice
							block.add(addInput(viewCopy, choice, results, errors, success, blocker));
							
							// Short-circuit on success
							block.add(Jump.to(onSuccess, Jump.Target.START, Condition.isTrue(success)));

							block.add(hookCall(ParserHook.CHOICE_AFTER_INPUT, Constant.of(choice.toString()), Constant.of(false)));
						}
					});
				} // else: this node type is not accepted here
			}
			handler.add(tokenTest);
			
			// Token was not in predict set or the prediction failed to parse
			// (this is currently only used for error recovery)
			handler.add(fallback);
			
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
				var partHandler = Block.create("part " + i);
				partHandler.add(hookCall(ParserHook.COMPOUND_BEFORE_PART, Constant.of(part.toString()), Constant.of(i)));
				
				partHandler.add(addInput(viewCopy, part, results, errors, success, currentBlocker));
				// Jump to end on failure (short-circuit)
				partHandler.add(Jump.to(handler, Jump.Target.END, Condition.isFalse(success)));
				
				// TODO what about failure hook?
				partHandler.add(hookCall(ParserHook.COMPOUND_AFTER_PART, Constant.of(part.toString()), Constant.of(i), Constant.of(true)));
				handler.add(partHandler);
				
				// Allow right recursion
				currentBlocker = rightBlocker;
			}
			// If all parts were found, advance the parent view
			handler.add(ADVANCE_VIEW.call(view, viewCopy));
			
			return handler;
		} else if (input instanceof RepeatingInput repeating) {
			var handler = Block.create("repeating");
			
			// TODO code4jvm: callback-based API for LoopBlock?
			var viewCopy = handler.add(COPY_VIEW.call(view));
			var body = Block.create("repeating");
			var loop = LoopBlock.whileLoop(body, Condition.always(true));
			// TODO this breaks left recursion elimination, does that matter?
			body.add(addInput(viewCopy, repeating.input(), results, errors, success, blocker.pop(handler)));
			
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
			
			// Insert a check for blocked node to generated code
			var isBlocked = handler.add(blocker.check(nodeRegistry.getTypeId(childNode.type())));
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
			var node = handler.add(parser.call(viewCopy, blocker.mask(), errors.errorSet()));
			
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
			var node = handler.add(parser.call(view, blocker.mask(), blocker.topNode(), errors.errorSet()));
			
			// Check for MISSING node; consider it a success, but store null for consistency
			var successTest = new IfBlock();
			successTest.branch(block -> {
				var missing = block.add(AST_NODE.getStatic(AST_NODE, "MISSING"));
				return Condition.refEqual(missing, node);
			}, block -> {
				block.add(results.setResult(virtualNode.inputId(), Constant.nullValue(AST_NODE)));
				block.add(success.set(Constant.of(true)));
			});
			
			// If we did not receive that, check for non-null node (success) and store that
			successTest.branch(Condition.isNull(node).not(), block -> {
				block.add(results.setResult(virtualNode.inputId(), node));
				block.add(success.set(Constant.of(true)));
			});
			
			handler.add(successTest);
			
			return handler;

		} else if (input instanceof WrapperInput wrapper) {
			var handler = Block.create("handle error " + wrapper.errorType());
			
			if (wrapper.input() != null) {
				// If this has child input, emit it
				var viewCopy = handler.add(COPY_VIEW.call(view));
				handler.add(addInput(viewCopy, wrapper.input(), results, errors, success, blocker));
				
				var successTest = new IfBlock();
				successTest.branch(Condition.isTrue(success), block -> {
					// No error, advance and continue
					block.add(ADVANCE_VIEW.call(view, viewCopy));
				});
				if (wrapper.isError()) {
					// On error, continue and optionally record it
					successTest.fallback(block -> {
						block.add(errors.errorAtHere(wrapper.errorType(), view));
						block.add(success.set(Constant.of(true)));
					});
				}
				handler.add(successTest);
			} else {
				// Unconditional marker
				if (wrapper.isError()) {
					// Mark error if this is an error
					handler.add(errors.errorAtHere(wrapper.errorType(), view));
				}
				handler.add(success.set(Constant.of(true)));
			}
			
			return handler;
		} else {
			throw new AssertionError("unknown input type: " + input);
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
		
		// Take the error list given to us as argument
		var errors = new ErrorManager(method.arg(SET));
		
		// Handle the root input
		method.add(addInput(view, input, results, errors, success, blocker));
		
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
		
		// Take the error list given to us as argument
		var errors = new ErrorManager(method.arg(SET));
		
		// Handle the root input
		method.add(addInput(view, input.input(), results, errors, success, blocker));
		
		// Create and return AST node if we have no failures
		var successTest = new IfBlock();
		successTest.branch(Condition.isTrue(success), block -> {
			var astNode = results.constructorArgs().get(0);
			var nullTest = new IfBlock();
			nullTest.branch(Condition.isNull(astNode), inner -> {
				// We got a success, but the AST node is null; there are probably compilation errors
				// We'll still need to signal caller to continue
				var missing = inner.add(AST_NODE.getStatic(AST_NODE, "MISSING"));
				inner.add(Return.value(missing));
			});
			block.add(nullTest);
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
