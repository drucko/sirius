/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.di;

import com.google.common.collect.Sets;
import sirius.kernel.commons.MultiMap;
import sirius.kernel.commons.Strings;
import sirius.kernel.commons.Tuple;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * An instance of PartRegistry is kept by {@link sirius.kernel.di.Injector} to track all registered
 * parts.
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/08
 */
class PartRegistry implements MutableGlobalContext {

    /**
     * Contains all registered parts
     */
    private final MultiMap<Class<?>, Object> parts = MultiMap.createSynchronized();

    /**
     * Contains all registered parts with a unique name. These parts will also
     * be contained in parts. This is just a lookup map if searched by unique
     * name.
     */
    private final Map<Class<?>, Map<String, Object>> namedParts = Collections.synchronizedMap(new HashMap<Class<?>, Map<String, Object>>());

    @SuppressWarnings("unchecked")
    @Override
    public <P> P getPart(Class<P> clazz) {
        Collection<?> items = parts.get(clazz);
        if (items.isEmpty()) {
            return null;
        }
        return (P) items.iterator().next();
    }

    @Override
    public <P> PartCollection<P> getPartCollection(final Class<P> partInterface) {
        return new PartCollection<P>() {

            @Override
            public Class<P> getInterface() {
                return partInterface;
            }

            @Override
            public Collection<P> getParts() {
                return PartRegistry.this.getParts(partInterface);
            }

            @Override
            public Iterator<P> iterator() {
                return getParts().iterator();
            }
        };
    }

    @SuppressWarnings("unchecked")
    @Override
    public <P> Collection<P> getParts(Class<P> partInterface) {
        return (Collection<P>) parts.get(partInterface);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <P> Collection<Tuple<String, P>> getNamedParts(@Nonnull Class<P> partInterface) {
        return (Collection<Tuple<String, P>>) (Object)Tuple.fromMap(namedParts.get(partInterface));
    }

    @Override
    public <T> T wire(T object) {
        // Wire....
        Class<?> clazz = object.getClass();
        while (clazz != null) {
            wireClass(clazz, object);
            clazz = clazz.getSuperclass();
        }

        return object;
    }

    /*
     * Called to initialize all static fields with annotations
     */
    void wireClass(Class<?> clazz) {
        while (clazz != null) {
            wireClass(clazz, null);
            clazz = clazz.getSuperclass();
        }
    }

    /*
     * Called to initialize all field of the given class in the given object
     */
    private void wireClass(Class<?> clazz, Object object) {
        for (Field field : clazz.getDeclaredFields()) {
            if ((!Modifier.isFinal(field.getModifiers())) && (object != null || Modifier.isStatic(field.getModifiers()))) {
                field.setAccessible(true);
                for (AnnotationProcessor proc : getParts(AnnotationProcessor.class)) {
                    if (field.isAnnotationPresent(proc.getTrigger())) {
                        try {
                            proc.handle(this, object, field);
                        } catch (Throwable e) {
                            Injector.LOG
                                    .WARN("Cannot process annotation %s on %s.%s: %s (%s)",
                                          proc.getTrigger().getName(),
                                          field.getDeclaringClass().getName(),
                                          field.getName(),
                                          e.getMessage(),
                                          e.getClass().getName());
                        }
                    }
                }
            }
        }
    }

    @Override
    public void registerPart(Object part, Class<?>... implementedInterfaces) {
        for (Class<?> iFace : implementedInterfaces) {
            parts.put(iFace, part);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <P> P getPart(String uniqueName, Class<P> clazz) {
        Map<String, Object> partsOfClass = namedParts.get(clazz);
        if (partsOfClass == null) {
            return null;
        }
        return (P) partsOfClass.get(uniqueName);
    }

    @Override
    public synchronized void registerPart(String uniqueName, Object part, Class<?>... implementedInterfaces) {
        for (Class<?> clazz : implementedInterfaces) {
            Map<String, Object> partsOfClass = namedParts.get(clazz);
            if (partsOfClass != null) {
                if (partsOfClass.containsKey(uniqueName)) {
                    throw new IllegalArgumentException(Strings.apply(
                            "The part '%s' cannot be registered as '%s' for class '%s'. The id is already taken by: %s (%s)",
                            part,
                            clazz.getName(),
                            uniqueName,
                            partsOfClass.get(uniqueName),
                            partsOfClass.get(uniqueName).getClass().getName()));
                }
            } else {
                partsOfClass = Collections.synchronizedMap(new TreeMap<String, Object>());
                namedParts.put(clazz, partsOfClass);
            }
            partsOfClass.put(uniqueName, part);
            parts.put(clazz, part);
        }
    }

    @Override
    public <P> P findPart(String uniqueName, Class<P> clazz) {
        P part = getPart(uniqueName, clazz);
        if (part == null) {
            throw new NoSuchElementException(Strings.apply("Cannot find %s of type %s", uniqueName, clazz.getName()));
        }
        return part;
    }

    /**
     * Processes all annotations of all known parts.
     */
    void processAnnotations() {
        Set<Object> initializedObjects = Sets.newHashSet();
        for (Collection<?> val : parts.getUnderlyingMap().values()) {
            for (Object part : val) {
                wire(part);
                if (part instanceof Initializable && !initializedObjects.contains(part)) {
                    try {
                        initializedObjects.add(part);
                        ((Initializable) part).initialize();
                    } catch (Exception e) {
                        Injector.LOG.WARN("Error initializing %s (%s)", part, part.getClass().getName());
                        Injector.LOG.WARN(e);
                    }
                }
            }
        }

    }

}
