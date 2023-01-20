package fi.benjami.code4jvm.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.function.Supplier;

import org.junit.jupiter.params.ParameterizedTest;

import fi.benjami.code4jvm.Constant;
import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.block.Block;
import fi.benjami.code4jvm.config.CompileOptions;
import fi.benjami.code4jvm.flag.Access;
import fi.benjami.code4jvm.statement.DebugInfo;
import fi.benjami.code4jvm.statement.Return;
import fi.benjami.code4jvm.typedef.ClassDef;

public class DebugInfoTest {

	@ParameterizedTest
	@OptionsSource
	public void lineNumbers(CompileOptions opts) throws Throwable {
		// Adapted from BlockTest#simpleBlock
		var def = ClassDef.create("fi.benjami.code4jvm.test.LineNumbers", Access.PUBLIC);
		def.addEmptyConstructor(Access.PUBLIC);
		def.interfaces(Type.of(Supplier.class));
		
		var method = def.addMethod(Type.OBJECT, "get", Access.PUBLIC);
		var block = Block.create();
		block.add(DebugInfo.lineNumber(1));
		block.add(Return.value(Constant.of("ok")));
		block.add(DebugInfo.lineNumber(2));
		method.add(block);
		method.add(Return.nothing()); // Verifier should error if this is reachable
		
		var instance = (Supplier<?>) TestUtils.newInstance(def, opts);
		assertEquals("ok", instance.get());
	}
}
