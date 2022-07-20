package fi.benjami.code4jvm.internal;

import fi.benjami.code4jvm.block.CompileContext;

public record MethodCompilerState(
		/**
		 * Compile context that is exposed in public API.
		 */
		CompileContext ctx,
		
		SlotAllocator slotAllocator,
		
		FrameManager frames
) {}
