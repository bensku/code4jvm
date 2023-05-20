package fi.benjami.code4jvm.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;

import fi.benjami.code4jvm.Constant;
import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.config.CompileOptions;
import fi.benjami.code4jvm.config.CoreOptions;
import fi.benjami.code4jvm.config.JavaVersion;
import fi.benjami.code4jvm.flag.Access;
import fi.benjami.code4jvm.statement.Return;
import fi.benjami.code4jvm.typedef.ClassDef;

@ExtendWith({EnableDebugExtension.class})
public class ConstantDynamicTest {

	private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
	
	@ParameterizedTest
	@OptionsSource
	public void classData(CompileOptions opts) throws Throwable {
		Assumptions.assumeTrue(opts.get(CoreOptions.JAVA_VERSION).isAtLeast(JavaVersion.JAVA_16));
		
		var def = ClassDef.create("fi.benjami.code4jvm.test.ConstantClassData", Access.PUBLIC);
		def.addEmptyConstructor(Access.PUBLIC);
		def.interfaces(Type.of(Supplier.class));
		
		var method = def.addMethod(Type.OBJECT, "get", Access.PUBLIC);
		method.add(Return.value(Constant.classData(Type.OBJECT)));
		
		var classData = new Object();
		var code = def.compile(opts);
		var instance = (Supplier<?>) TestUtils.newInstance(LOOKUP.defineHiddenClassWithClassData(code, classData, true));
		assertEquals(classData, instance.get());
	}
	
	@ParameterizedTest
	@OptionsSource
	public void classDataAt(CompileOptions opts) throws Throwable {
		Assumptions.assumeTrue(opts.get(CoreOptions.JAVA_VERSION).isAtLeast(JavaVersion.JAVA_16));
		
		var def = ClassDef.create("fi.benjami.code4jvm.test.ConstantClassDataAt", Access.PUBLIC);
		def.addEmptyConstructor(Access.PUBLIC);
		def.interfaces(Type.of(IntSupplier.class));
		
		var method = def.addMethod(Type.INT, "getAsInt", Access.PUBLIC);
		method.add(Return.value(Constant.classDataAt(Type.INT, 0)));
		
		var classData = List.of(10);
		var code = def.compile(opts);
		var instance = (IntSupplier) TestUtils.newInstance(LOOKUP.defineHiddenClassWithClassData(code, classData, true));
		assertEquals(10, instance.getAsInt());
	}
}
