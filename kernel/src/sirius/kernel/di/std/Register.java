package sirius.kernel.di.std;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a given class as "self registering".
 * <p>
 * If a non empty name is given, the part is registered with the given name and
 * for the given classes. Otherwise, the part is directly registered without any
 * unique name.
 * </p>
 * <p>
 * If no <tt>classes</tt> are given, the class is registered for its own class, and all implemented interfaces. This
 * is probably the right choice in many situations, therefore this annotation can be used without any parameters in
 * most cases.
 * </p>
 * <p>
 * Classes wearing this annotations must have a zero args constructor.
 * </p>
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/08
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Register {

    /**
     * Unique name of the part.
     */
    String name() default "";

    /**
     * Classes for which the part is registered.
     */
    Class<?>[] classes() default {};
}
