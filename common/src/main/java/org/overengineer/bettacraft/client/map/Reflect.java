package org.overengineer.bettacraft.client.map;

import java.lang.reflect.Method;

/** M53: small reflection helpers for the map mod adapters (public methods by name and argument count, also of hidden classes). */
final class Reflect {
    private Reflect() {}

    static Object call(Object target, String name, Object... args) throws ReflectiveOperationException {
        Method m = find(target.getClass(), name, args);
        if (m == null) throw new NoSuchMethodException(target.getClass().getName() + "#" + name + "/" + args.length);
        m.setAccessible(true);
        return m.invoke(target, args);
    }

    private static Method find(Class<?> c, String name, Object[] args) {
        for (Class<?> k = c; k != null; k = k.getSuperclass()) {
            Method m = match(k.getDeclaredMethods(), name, args);
            if (m != null) return m;
            for (Class<?> i : k.getInterfaces()) {
                m = match(i.getMethods(), name, args);
                if (m != null) return m;
            }
        }
        return null;
    }

    private static Method match(Method[] methods, String name, Object[] args) {
        outer:
        for (Method m : methods) {
            if (!m.getName().equals(name) || m.getParameterCount() != args.length) continue;
            Class<?>[] p = m.getParameterTypes();
            for (int i = 0; i < p.length; i++) {
                if (args[i] == null) continue;
                Class<?> want = p[i].isPrimitive() ? box(p[i]) : p[i];
                if (!want.isInstance(args[i])) continue outer;
            }
            return m;
        }
        return null;
    }

    private static Class<?> box(Class<?> p) {
        if (p == int.class) return Integer.class;
        if (p == boolean.class) return Boolean.class;
        if (p == double.class) return Double.class;
        if (p == float.class) return Float.class;
        if (p == long.class) return Long.class;
        return p;
    }
}
