/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.web.health.console;

import sirius.kernel.cache.Cache;
import sirius.kernel.cache.CacheManager;
import sirius.kernel.di.std.Register;

/**
 * Console command which reports statistics for all caches.
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2014/01
 */
@Register(name = "cache")
public class CacheCommand implements Command {

    @Override
    public void execute(Output output, String... params) {
        if (params.length > 0) {
            output.apply("Flushing: %s", params[0]);
            output.blankLine();
        }
        output.apply("%-53s %8s %8s %8s", "NAME", "SIZE", "MAX-SIZE", "HIT-RATE");
        output.separator();
        for (Cache<?, ?> c : CacheManager.getCaches()) {
            output.apply("%-53s %8d %8d %8d", c.getName(), c.getSize(), c.getMaxSize(), c.getHitRate());
        }
        output.separator();
    }

    @Override
    public String getName() {
        return "cache";
    }

    @Override
    public String getDescription() {
        return "Lists all available caches. Add a name of a cache as parameter to flush it.";
    }

}
