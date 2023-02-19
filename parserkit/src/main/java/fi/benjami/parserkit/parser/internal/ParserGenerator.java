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
	
	public ParserGenerator(String className, NodeRegistry nodeRegistry, TokenType[] tokenTypes) {
		this.nodeRegistry = nodeRegistry;
		this.tokenTypes = tokenTypes;
		this.nodeParsers = new HashMap<>();
		this.def = ClassDef.create(className, Access.PUBLIC);
		def.interfaces(Type.of(Parser.class));
	}
	
	public void addRoot(Class<? extends AstNode> nodeType) {
		if (!nodeParsers.containsKey(nodeType)) {			
			var parser = addMethod(nodeType);
			nodeParsers.put(nodeType, parser);
		}
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
				var node = block.add(entry.getValue().call(view));
				block.add(Return.value(node));
			});
		}
		method.add(roots);
		method.add(Return.value(Constant.nullValue(AST_NODE)));
		
		// Add default constructor
		def.addEmptyConstructor(Access.PUBLIC);
		
		return def.compile();
	}
	
	private void addInput(Value view, Block handler, Input input, ResultRegistry results, Variable success) {		
		if (input instanceof TokenInput token) {
			// Consume one token and check if it is what we expected
			var nextToken = handler.add(POP_TOKEN.call(view));
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
		} else if (input instanceof ChoiceInput choices) {			
			var nextToken = handler.add(PEEK_TOKEN.call(view));
			var ordinal = handler.add(GET_TYPE.call(nextToken));
			
			// Try to select choice using the predict set
			// TODO use tableswitch for large tables?
			for (var type : tokenTypes) {
				var validChoices = choices.filterFor(nodeRegistry, type);
				if (validChoices.length != 0) {
					var tokenTest = new IfBlock();
					tokenTest.branch(Condition.equal(Constant.of(type.ordinal()), ordinal), block -> {
						for (var choice : validChoices) {
							// Take a copy of view, backtracing may be needed when there are multiple choices
							var viewCopy = block.add(COPY_VIEW.call(view));
							addInput(viewCopy, block, choice, results, success); // Parse the choice
							
							var successTest = new IfBlock();
							successTest.branch(Condition.isTrue(success), inner -> {
								// Advance parent view given to us if parsing succeeded
								inner.add(ADVANCE_VIEW.call(view, viewCopy));
								
								// Short-circuit on success by jumping to end
								inner.add(Jump.to(handler, Jump.Target.END));
							}); // On failure, continue to next choice
							block.add(successTest);
						}
					});
					handler.add(tokenTest);
					handler.add(Jump.to(handler, Jump.Target.END)); // Failure
				} // else: this node type is not accepted here
			}
		} else if (input instanceof CompoundInput compound) {
			var viewCopy = handler.add(COPY_VIEW.call(view));
			for (var part : compound.parts()) {
				var block = Block.create();
				addInput(viewCopy, block, part, results, success);
				// Jump to end on failure (short-circuit)
				block.add(Jump.to(handler, Jump.Target.END, Condition.isFalse(success)));
				handler.add(block);
			}
			// If all parts were found, advance the parent view
			handler.add(ADVANCE_VIEW.call(view, viewCopy));
		} else if (input instanceof RepeatingInput repeating) {
			// TODO callback-based API for LoopBlock?
			var viewCopy = handler.add(COPY_VIEW.call(view));
			var body = Block.create();
			var loop = LoopBlock.whileLoop(body, Condition.always(true));
			addInput(viewCopy, body, repeating.input(), results, success);
			
			var successTest = new IfBlock();
			successTest.branch(Condition.isTrue(success), block -> {
				// Advance view given to us if parsing succeeded
				block.add(ADVANCE_VIEW.call(view, viewCopy));
				block.add(loop.breakStmt());
			});
			successTest.fallback(block -> {
				// Repeating zero times is perfectly ok -> success
				block.add(success.set(Constant.of(true)));
			});
			body.add(successTest);
			
			handler.add(loop);
		} else if (input instanceof ChildNodeInput childNode) {
			var generator = nodeParsers.get(childNode.type());
			if (generator == null) {
				generator = addMethod(childNode.type());
				nodeParsers.put(childNode.type(), generator);
			}
			// Call the method
			var node = handler.add(generator.call(view));
			
			// Check success (node == null on failure) and store the node
			var successTest = new IfBlock();
			successTest.branch(Condition.isNull(node).not(), block -> {
				block.add(results.setResult(childNode.inputId(), node));
				block.add(success.set(Constant.of(true)));
			});
			successTest.fallback(block -> {
				block.add(success.set(Constant.of(false)));
			});
			handler.add(successTest);
		}
	}
	
	private CallTarget addMethod(Class<? extends AstNode> nodeType) {
		var input = nodeRegistry.getRootInput(nodeType);
		
		var name = "parser$" + def.methods().size();
		var method = def.addMethod(AST_NODE, name, Access.PRIVATE);
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
			throw new AssertionError(); // TODO
		}
		var inputArgs = findInputs(constructor);
		
		// Prepare local variables for results
		var results = new ResultRegistry(inputArgs);
		method.add(results.initResults());
		
		// Handle the root input
		var handler = Block.create();
		addInput(view, handler, input, results, success);
		method.add(handler);
		
		// Call AST node constructor and return the result
		var astNode = method.add(Type.of(nodeType).newInstance(results.constructorArgs().toArray(Value[]::new)));
		method.add(Return.value(astNode.asType(AST_NODE)));
		return def.type().privateMethod(AST_NODE, name, TOKEN_VIEW);
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
					block.add(variable.set(Constant.nullValue(variable.type())));
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

}
