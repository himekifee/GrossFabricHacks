package net.devtech.grossfabrichacks.more_enums;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import user11681.reflect.Fields;
import user11681.reflect.Invoker;

public class EnumAdder {
	private static final Field ENUM_CONSTANT_DIRECTORY;
	private static final Field ENUM_CONSTANTS;
	private static final Field CONSTRUCTOR_ACCESSOR;
	private static final MethodHandle AQUIRE_CONSTRUCTOR_ACCESSOR;
	static {
		ENUM_CONSTANT_DIRECTORY = Fields.field(Class.class, "enumConstantDirectory");
		ENUM_CONSTANTS = Fields.field(Class.class, "enumConstants");
		CONSTRUCTOR_ACCESSOR = Fields.field(Constructor.class, "constructorAccessor");
		AQUIRE_CONSTRUCTOR_ACCESSOR = Invoker.unreflect(Constructor.class, "acquireConstructorAccessor");
	}

	/**
	 * @param values {@link net.minecraft.util.math.Direction} = field_11037, you need to find the name of the synthetic field that holds the values()
	 * 		array
	 */
/*
	public static <T extends Enum<T>> T add(String values, Class<T> type, Object... args) {
		try {
			Field field = type.getDeclaredField(values);
			if (!field.isSynthetic() && !field.getType()
			                                  .isArray()) {
				throw new IllegalArgumentException("values[] is not synthetic and an array!");
			}
			if (!field.isAccessible()) {
				field.setAccessible(true);
			}

			Object[] arr = (Object[]) field.get(null);
			synchronized (arr) {
				Object[] copy = Arrays.copyOf(arr, arr.length + 1);
				T instance = brute(type, args);
				copy[arr.length] = instance;
				field.set(null, copy);
				// refresh values() cache
				ENUM_CONSTANT_DIRECTORY.set(type, null);
				return instance;
			}
		} catch (ReflectiveOperationException e) {
			throw GrossFabricHacks.Common.crash(e);
		}
	}

	*/
/**
	 * hahahyes brute force activated

	public static <T> T brute(Class<T> type, Object... args) {
		T instance = null;
		for (Constructor<?> constructor : type.getDeclaredConstructors()) {
			try {
				if (!constructor.isAccessible()) {
					constructor.setAccessible(true);
				}
				ConstructorAccessor accessor = get(constructor);
				instance = (T) accessor.newInstance(args);
			} catch (ReflectiveOperationException ignored) {
			}
		}
		if (instance == null) {
			throw new IllegalArgumentException("No valid constructor found!");
		}
		return instance;
	}

	public static ConstructorAccessor get(Constructor<?> ctor) throws IllegalAccessException, InvocationTargetException {
		ConstructorAccessor accessor = (ConstructorAccessor) CONSTRUCTOR_ACCESSOR.get(ctor);
		if(accessor != null) return accessor;
		return (ConstructorAccessor) AQUIRE_CONSTRUCTOR_ACCESSOR.invoke(ctor);
	}
 */
}
