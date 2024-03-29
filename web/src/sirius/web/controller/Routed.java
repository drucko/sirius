/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.web.controller;

import sirius.kernel.commons.PriorityCollector;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Attaches a URI to a {@link Controller} method.
 * <p>
 * For conflicting URIs like <code>/foo/:1</code> should handle everything but <code>/foo/special</code>, use a
 * priority below {@link PriorityCollector#DEFAULT_PRIORITY} for <code>/foo/special</code>
 * </p>
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/11
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Routed {
    /**
     * Sets the priority used for this route.
     * <p>
     * For non-conflicting URIs, this can be skipped. For everything else, the lower the better ;-)
     * </p>
     *
     * @return the priority used to sort the order of invocation of all rules.
     *         The default is  {@link PriorityCollector#DEFAULT_PRIORITY}.
     */
    int priority() default PriorityCollector.DEFAULT_PRIORITY;

    /**
     * Returns the URI pattern which describes which request should be handled. Parameters can be placed by
     * using :1, :2 etc. Therefore <code>/foo/:1</code> matches <code>/foo/test, /foo/hello</code>
     *
     * @return the URI pattern describing which requests to handle
     */
    String value();

    /**
     * Determines if the annotated method supports pre-dispatching.
     * <p>
     * A pre-dispatchable method takes care of the requests payload itself. Therefore the method must declare an
     * additional parameter of type {@link sirius.web.http.InputStreamHandler} which will be used to consume
     * the data sent via POST or PUT.
     * </p>
     *
     * @return <tt>true</tt> if the method is pre-dispatchable, <tt>false</tt> otherwise
     */
    boolean preDispatchable() default false;
}
