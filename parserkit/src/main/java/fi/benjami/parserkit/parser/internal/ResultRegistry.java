package fi.benjami.parserkit.parser.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fi.benjami.code4jvm.Constant;
import fi.benjami.code4jvm.Statement;
import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.Value;
import fi.benjami.code4jvm.Variable;
import fi.benjami.code4jvm.statement.NoOp;

public class ResultRegistry {
		
	record InputArg(String inputId, Class<?> type) {}
	
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
				if (variable.type().equals(ParserGenerator.LIST)) {
					var list = block.add(ParserGenerator.ARRAY_LIST.newInstance());
					block.add(variable.set(list.asType(ParserGenerator.LIST)));
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
			if (target.type().equals(ParserGenerator.LIST)) {
				return ParserGenerator.LIST_ADD.call(target, value);
			} else {					
				return target.set(value.cast(target.type()));
			}
		}
	}
	
	public List<Variable> constructorArgs() {
		return inputList;
	}
}
