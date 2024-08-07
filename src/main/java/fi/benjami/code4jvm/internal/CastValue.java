package fi.benjami.code4jvm.internal;

import java.util.Map;
import java.util.Optional;

import org.objectweb.asm.MethodVisitor;

import fi.benjami.code4jvm.Type;
import fi.benjami.code4jvm.Value;
import fi.benjami.code4jvm.util.TypeUtils;

import static org.objectweb.asm.Opcodes.*;

public class CastValue implements Value {
	
	private static final int OBJECT_OBJECT = 1,
			INT_BYTE = 1 << 1,
			INT_SHORT = 1 << 2,
			INT_CHAR = 1 << 3,
			INT_LONG = 1 << 4,
			INT_FLOAT = 1 << 5,
			INT_DOUBLE = 1 << 6,
			LONG_INT = 1 << 7,
			LONG_FLOAT = 1 << 8,
			LONG_DOUBLE = 1 << 9,
			FLOAT_INT = 1 << 10,
			FLOAT_LONG = 1 << 11,
			FLOAT_DOUBLE = 1 << 12,
			DOUBLE_INT = 1 << 13,
			DOUBLE_LONG = 1 << 14,
			DOUBLE_FLOAT = 1 << 15,
			UNBOX = 1 << 16,
			BOX = 1 << 17;
	
	private static final Map<Type, Type> PRIMITIVE_TO_BOXED = Map.of(
			Type.BOOLEAN, Type.of(Boolean.class),
			Type.BYTE, Type.of(Byte.class),
			Type.SHORT, Type.of(Short.class),
			Type.CHAR, Type.of(Character.class),
			Type.INT, Type.of(Integer.class),
			Type.LONG, Type.of(Long.class),
			Type.FLOAT, Type.of(Float.class),
			Type.DOUBLE, Type.of(Double.class)
			);
	
	private static final Map<Type, Type> BOXED_TO_PRIMITIVE = Map.of(
			Type.of(Boolean.class), Type.BOOLEAN,
			Type.of(Byte.class), Type.BYTE,
			Type.of(Short.class), Type.SHORT,
			Type.of(Character.class), Type.CHAR,
			Type.of(Integer.class), Type.INT,
			Type.of(Long.class), Type.LONG,
			Type.of(Float.class), Type.FLOAT,
			Type.of(Double.class), Type.DOUBLE
			);
		
	public static Value cast(Value original, Type to) {
		assert original != null;
		assert to != null;
		
		// Check if cast is actually needed
		var from = original.type();
		if (from.equals(to)) {
			return original;
		}
		
		// CastValue supports multiple casts (e.g. float -> int -> byte)
		if (original instanceof CastValue cast) {
			original = cast.original();
		}
		
		int cast = 0;
		if (from.isObject() && to.isPrimitive()) {
			// Unbox can directly target different primitive type
			cast |= UNBOX;
			if (!from.equals(Type.OBJECT) && !BOXED_TO_PRIMITIVE.containsKey(from)) {
				throw new UnsupportedOperationException("cannot cast " + from + " to " + to);
			}
		} else if (to.equals(Type.BYTE)) {
			cast |= convertToInt(from);
			cast |= INT_BYTE;
		} else if (to.equals(Type.SHORT)) {
			cast |= convertToInt(from);
			cast |= INT_SHORT;
		} else if (to.equals(Type.CHAR)) {
			cast |= convertToInt(from);
			cast |= INT_CHAR;
		} else if (to.equals(Type.INT)) {
			cast |= convertToInt(from);
		} else if (to.equals(Type.LONG)) {
			if (TypeUtils.isIntLike(from)) {
				cast |= INT_LONG;
			} else if (from.equals(Type.FLOAT)) {
				cast |= FLOAT_LONG;
			} else if (from.equals(Type.DOUBLE)) {
				cast |= DOUBLE_LONG;
			}
		} else if (to.equals(Type.FLOAT)) {
			if (TypeUtils.isIntLike(from)) {
				cast |= INT_FLOAT;
			} else if (from.equals(Type.LONG)) {
				cast |= LONG_FLOAT;
			} else if (from.equals(Type.DOUBLE)) {
				cast |= DOUBLE_FLOAT;
			}
		} else if (to.equals(Type.DOUBLE)) {
			if (TypeUtils.isIntLike(from)) {
				cast |= INT_DOUBLE;
			} else if (from.equals(Type.LONG)) {
				cast |= LONG_DOUBLE;
			} else if (from.equals(Type.FLOAT)) {
				cast |= FLOAT_DOUBLE;
			}
		} else {
			// Object type
			if (!from.isPrimitive()) {
				// Cast object type to another, unless the target is super of everything
				if (!to.equals(Type.OBJECT)) {					
					cast |= OBJECT_OBJECT;
				}
			} else {
				// Try to box a primitive type
				cast |= BOX;
				// TODO non-booleans -> j.l.Number
				if (!to.equals(Type.OBJECT)) {
					var expectedPrimitive = BOXED_TO_PRIMITIVE.get(from);
					if (expectedPrimitive == null) {
						throw new UnsupportedOperationException("cannot cast " + from + " to " + to);
					}
					if (!from.equals(expectedPrimitive)) {
						// Cannot directly box e.g. int into j.l.Long
						original = cast(original, expectedPrimitive);
					}
				}
			}
		}
		
		return new CastValue(original, to, cast);
	}
	
	public static CastValue fakeCast(Value original, Type to) {
		// Don't actually cast anything!
		return new CastValue(original, to, 0);
	}
	
	private static int convertToInt(Type from) {
		if (TypeUtils.isIntLike(from)) {
			// JVM treats all integer types except longs as ints (except in arrays)
			// The casts from ints to smaller types exist only for truncation
			return 0;
		} else if (from.equals(Type.LONG)) {
			return LONG_INT;
		} else if (from.equals(Type.FLOAT)) {
			return FLOAT_INT;
		} else if (from.equals(Type.DOUBLE)) {
			return DOUBLE_INT;
		} else {
			// If unboxing is possible, do it and convert from unboxed type to int
			return UNBOX | convertToInt(unboxType(from));
		}
	}
	
	private static int unboxIfNeeded(Type type) {
		return type.isPrimitive() ? 0 : UNBOX;
	}
	
	private static Type unboxType(Type type) {
		if (type.isPrimitive()) {
			return type; // Already primitive type
		}
		return BOXED_TO_PRIMITIVE.get(type);
	}

	private final Value original;
	private final Type type;
	private final int cast;
	
	private CastValue(Value original, Type type, int cast) {
		this.original = original;
		this.type = type;
		this.cast = cast;
	}
	
	@Override
	public Type type() {
		return type;
	}

	@Override
	public Optional<String> name() {
		return original.name();
	}
	
	@Override
	public Value original() {
		return original.original();
	}
	
	public void emitCast(MethodVisitor mv) {
		if (cast == 0) {
			return;
		}
		// FIXME this is VERY fishy, need better tests
		// The cast order is important!
		// 1. Unboxing
		// 2. Casts to int
		// 3. Everything else
		if ((cast & UNBOX) != 0) {
			// FIXME unusual boolean and char casts
			var boxedType = original.type();
			if (boxedType.equals(Type.OBJECT)) {
				// j.l.Object doesn't have intValue() and friends
				boxedType = PRIMITIVE_TO_BOXED.get(type);
				mv.visitTypeInsn(CHECKCAST, boxedType.internalName());
			}
			mv.visitMethodInsn(INVOKEVIRTUAL, boxedType.internalName(), type.name() + "Value",
					TypeUtils.methodDescriptor(type), false);
		} else if ((cast & LONG_INT) != 0) {
			mv.visitInsn(L2I);
		} else if ((cast & FLOAT_INT) != 0) {
			mv.visitInsn(F2I);
		} else if ((cast & DOUBLE_INT) != 0) {
			mv.visitInsn(D2I);
		} else if ((cast & INT_BYTE) != 0) {
			mv.visitInsn(I2B);
		} else if ((cast & INT_SHORT) != 0) {
			mv.visitInsn(I2S);
		} else if ((cast & INT_CHAR) != 0) {
			mv.visitInsn(I2C);
		} else if ((cast & INT_LONG) != 0) {
			mv.visitInsn(I2L);
		} else if ((cast & INT_FLOAT) != 0) {
			mv.visitInsn(I2F);
		} else if ((cast & INT_DOUBLE) != 0) {
			mv.visitInsn(I2D);
		} else if ((cast & LONG_FLOAT) != 0) {
			mv.visitInsn(L2F);
		} else if ((cast & LONG_DOUBLE) != 0) {
			mv.visitInsn(L2D);
		} else if ((cast & FLOAT_LONG) != 0) {
			mv.visitInsn(F2L);
		} else if ((cast & FLOAT_DOUBLE) != 0) {
			mv.visitInsn(F2D);
		} else if ((cast & DOUBLE_LONG) != 0) {
			mv.visitInsn(D2L);
		} else if ((cast & DOUBLE_FLOAT) != 0) {
			mv.visitInsn(D2F);
		} else if ((cast & BOX) != 0) {
			var boxedType = type.equals(Type.OBJECT) ? PRIMITIVE_TO_BOXED.get(original.type()) : type;
			mv.visitMethodInsn(INVOKESTATIC, boxedType.internalName(), "valueOf", TypeUtils.methodDescriptor(boxedType, original.type()), false);
		} else if ((cast & OBJECT_OBJECT) != 0) {
			// For some reason, JVM expects descriptor for arrays but NOT for anything else
			mv.visitTypeInsn(CHECKCAST, type.isArray() ? type.descriptor() : type.internalName());
		}
	}
	
	@Override
	public String toString() {
		return "(" + type + ") " + original;
	}

}
