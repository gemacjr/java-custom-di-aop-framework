package com.example.aop;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Wraps a target in a JDK dynamic proxy that applies "around advice" to methods annotated with
 * {@link LogExecutionTime}.
 *
 * <p>JDK proxies can only implement interfaces, so {@code interfaceType} must be an interface.
 * (Proxying concrete classes needs bytecode generation such as CGLIB or ByteBuddy.)
 */
public final class AOPProxy {
    private AOPProxy() {}

    @SuppressWarnings("unchecked")
    public static <T> T createProxy(T target, Class<T> interfaceType) {
        if (!interfaceType.isInterface()) {
            throw new IllegalArgumentException(interfaceType.getName() + " is not an interface");
        }
        return (T) Proxy.newProxyInstance(
                interfaceType.getClassLoader(),
                new Class<?>[]{interfaceType},
                (proxy, method, args) -> {
                    if (!isAnnotated(target, method)) {
                        return invoke(target, method, args);
                    }
                    long start = System.nanoTime();
                    try {
                        return invoke(target, method, args);
                    } finally {
                        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
                        System.out.println("Execution time of " + method.getName() + ": " + elapsedMs + "ms");
                    }
                });
    }

    /**
     * {@code method} is the interface method, so check the interface first and then the
     * implementation, since users usually annotate the implementation.
     */
    private static boolean isAnnotated(Object target, Method method) {
        if (method.isAnnotationPresent(LogExecutionTime.class)) {
            return true;
        }
        try {
            return target.getClass()
                    .getMethod(method.getName(), method.getParameterTypes())
                    .isAnnotationPresent(LogExecutionTime.class);
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    /** Rethrows the target's own exception instead of a reflective InvocationTargetException wrapper. */
    private static Object invoke(Object target, Method method, Object[] args) throws Throwable {
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }
}
