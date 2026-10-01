package com.example.di;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * A minimal dependency-injection container.
 *
 * <p>Phase 1 instantiates every {@link Service}-annotated class. Phase 2 walks each instance's
 * {@link Inject} fields and wires them. Splitting the phases means declaration order doesn't matter
 * and circular field references work, since every instance exists before any wiring happens.
 */
public class Container {
    /** Lookup key (concrete class or one of its interfaces) -> singleton instance. */
    private final Map<Class<?>, Object> services = new HashMap<>();
    /** Each distinct instance once, for the wiring pass. */
    private final Set<Object> instances = new LinkedHashSet<>();

    public Container(Class<?>... classes) throws Exception {
        for (Class<?> clazz : classes) {
            if (clazz.isAnnotationPresent(Service.class)) {
                register(clazz, clazz.getDeclaredConstructor().newInstance());
            }
        }
        for (Object service : instances) {
            for (Field field : service.getClass().getDeclaredFields()) {
                if (field.isAnnotationPresent(Inject.class)) {
                    field.setAccessible(true);
                    field.set(service, resolveDependency(service.getClass(), field));
                }
            }
        }
    }

    /** Registers the instance under its concrete class and every interface it implements. */
    private void register(Class<?> clazz, Object instance) {
        instances.add(instance);
        services.put(clazz, instance);
        for (Class<?> iface : clazz.getInterfaces()) {
            services.put(iface, instance);
        }
    }

    /**
     * Finds the service to inject into {@code field} of {@code owner}.
     *
     * <p>Fails fast at container startup rather than injecting {@code null}, which would only
     * surface later as a NullPointerException far from the misconfiguration.
     */
    private Object resolveDependency(Class<?> owner, Field field) {
        Object dependency = services.get(field.getType());
        if (dependency == null) {
            throw new IllegalStateException(String.format(
                    "Cannot inject field '%s' of %s: no @Service registered for type %s. "
                            + "Pass an implementation annotated with @Service to the Container.",
                    field.getName(), owner.getName(), field.getType().getName()));
        }
        return dependency;
    }

    public <T> T getService(Class<T> clazz) {
        return clazz.cast(services.get(clazz));
    }
}
