/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.cache;

import sirius.kernel.commons.ValueProvider;

import javax.annotation.Nullable;

/**
 * Caches a single value to prevent frequent re-computation.
 * <p>
 * Caches a computed value for a certain amount of time. Re-computes the value once the value is expired and the
 * cache is used again.
 * </p>
 * <p>
 * A real lookup cache, with a Map like behaviour can be found here: {@link Cache}.
 * </p>
 * <p>
 * Use {@link CacheManager#createInlineCache(long, java.util.concurrent.TimeUnit, sirius.kernel.commons.ValueProvider)}
 * to create a new inline cache.
 * </p>
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 1.0
 */
public class InlineCache<E> {
    private E buffer;
    private long lastComputation;
    private long timeout;
    private ValueProvider<E> computer;

    /**
     * Creates a new inline cache based on the given parameters.
     */
    protected InlineCache(@Nullable ValueProvider<E> computer, long timeout) {
        this.computer = computer;
        this.timeout = timeout;
    }

    /**
     * Either returns a cached value or computes a new one, if no valid value is in the cache.
     *
     * @return a cached or computed value, generated by the internal <tt>ValueProvider</tt>
     */
    @Nullable
    public E get() {
        if (System.currentTimeMillis() - lastComputation > timeout) {
            buffer = computer.get();
            lastComputation = System.currentTimeMillis();
        }
        return buffer;
    }

    /**
     * Forces the cache to reset and re-compute its internal value on the next access
     */
    public void flush() {
        lastComputation = 0;
    }
}
