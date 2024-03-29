/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Provides a pull pattern for asking methods to compute or fill a <tt>List</tt>.
 * <p>
 * When asking methods to create or populate a {@link List} it's easier to create and pass along a
 * <tt>Collector</tt> instead of having each method creating its own list and joining them afterwards.
 * </p>
 * <p>
 * By subclassing <tt>Collector</tt> one can also directly process the given values instead of just storing them
 * in a list.
 * </p>
 * <p>
 * A typical use-case is:
 * <code>
 * <pre>
 *             Collector&lt;String&gt; collector = new Collector&lt;String&gt;();
 *             computeStrings1(collector);
 *             computeStrings2(collector);
 *
 *             collector.getData(); // use values
 *         </pre>
 * </code>
 * </p>
 * <p>
 * If a sorted list is required which does not depend on insertion order but on a given priority of each entry
 * {@link PriorityCollector} can be used.
 * </p>
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @see PriorityCollector
 * @since 2013/08
 */
public class Collector<T> {

    /**
     * Creates a new <tt>Collector</tt>.
     * <p>
     * Boilerplate method, so one doesn't need to re-type the type parameters.
     * </p>
     *
     * @return a new <tt>Collector</tt>
     */
    public static <T> Collector<T> create() {
        return new Collector<T>();
    }

    private List<T> data = new ArrayList<T>();

    /**
     * Adds a value to the collector
     *
     * @param entity contains the value to be added to the collector.
     */
    public void add(T entity) {
        data.add(entity);
    }


    /**
     * Adds all values of the given collection to the collector
     *
     * @param entities the collection of values added to the collector.
     */
    public void addAll(@Nonnull Collection<? extends T> entities) {
        data.addAll(entities);
    }

    /**
     * Returns the <tt>List</tt> of values which where added to the collector so far.
     * <p>
     * For the sake of simplicity, this returns the internally used list. Therefore modifying this list, modifies
     * the collector.
     * </p>
     *
     * @return the list of values supplied to the collector so far.
     */
    @Nonnull
    public List<T> getData() {
        return data;
    }

    @Override
    public String toString() {
        return data.toString();
    }

}
