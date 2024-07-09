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
}
