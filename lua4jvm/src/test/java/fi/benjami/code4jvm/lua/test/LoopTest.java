package fi.benjami.code4jvm.lua.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import fi.benjami.code4jvm.lua.LuaVm;
import fi.benjami.code4jvm.lua.runtime.LuaTable;

public class LoopTest {

	private final LuaVm vm = new LuaVm();
	
	@Test
	public void iteratorFor() throws Throwable {
		vm.execute("""
				local function testNext(state, ctrl)
					if ctrl == 10 then
						return nil, "foo"
					else
						return ctrl + 1, "foo"
					end
				end
				
				iTbl = {}
				for i in testNext, nil, 0 do
					iTbl[i] = true
				end
				""");
		var tbl = (LuaTable) vm.globals().get("iTbl");
		for (double i = 1; i < 10; i++) {
			assertEquals(true, tbl.get(i));
		}
	}
	
	@Test
	public void iteratorForNoMultival() throws Throwable {
		vm.execute("""
				local function testNext(state, ctrl)
					if ctrl == 10 then
						return nil
					else
						return ctrl + 1
					end
				end
				
				iTbl = {}
				for i in testNext, nil, 0 do
					iTbl[i] = true
				end
				""");
		var tbl = (LuaTable) vm.globals().get("iTbl");
		for (double i = 1; i < 10; i++) {
			assertEquals(true, tbl.get(i));
		}
	}
	
	@Test
	public void iteratorFromFunction() throws Throwable {
		vm.execute("""
				local function testNext(state, ctrl)
					if ctrl == 10 then
						return nil
					else
						return ctrl + 1
					end
				end
				
				local function testIterator()
					return testNext, nil, 0
				end
				
				iTbl = {}
				for i in testIterator() do
					iTbl[i] = true
				end
				""");
		var tbl = (LuaTable) vm.globals().get("iTbl");
		for (double i = 1; i < 10; i++) {
			assertEquals(true, tbl.get(i));
		}
	}
	
	@Test
	public void iteratorFromFunction2() throws Throwable {
		// Almost same as above, but testIterator() does not return multival
		vm.execute("""
				local function testNext(state, ctrl)
					if ctrl == nil then
						return 1
					elseif ctrl == 10 then
						return nil
					else
						return ctrl + 1
					end
				end
				
				local function testIterator()
					return testNext
				end
				
				iTbl = {}
				for i in testIterator() do
					iTbl[i] = true
				end
				""");
		var tbl = (LuaTable) vm.globals().get("iTbl");
		for (double i = 1; i < 10; i++) {
			assertEquals(true, tbl.get(i));
		}
	}
	
	@Test
	public void breakFor() throws Throwable {
		vm.execute("""
				local function testNext(state, ctrl)
					if ctrl == 10 then
						return nil
					else
						return ctrl + 1
					end
				end
				
				local function testIterator()
					return testNext, nil, 0
				end
				
				iTbl = {}
				for i in testIterator() do
					iTbl[i] = true
					break
				end
				""");
		var tbl = (LuaTable) vm.globals().get("iTbl");
		assertEquals(true, tbl.get(1d));
		assertEquals(null, tbl.get(2d));
	}
}
