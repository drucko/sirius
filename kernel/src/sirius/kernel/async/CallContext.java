/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.async;

import org.apache.log4j.MDC;
import sirius.kernel.Sirius;
import sirius.kernel.commons.Strings;
import sirius.kernel.commons.Tuple;
import sirius.kernel.commons.Value;
import sirius.kernel.commons.Watch;
import sirius.kernel.health.Counter;
import sirius.kernel.health.Exceptions;
import sirius.kernel.nls.NLS;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

/**
 * A CallContext is attached to each thread managed by sirius.
 * <p>
 * It provides access to different sub-contexts via {@link #get(Class)}. Also, it provides acces to the mapped
 * diagnostic context (MDC). This can be filled by various parts of the framework (like which request-uri is
 * currently being processed, which user is currently active etc.) and will be attached to each error. Also, each
 * context comes with a new "flow-id". This can be used to trace an execution across different threads and even
 * across different cluster nodes.
 * </p>
 * <p>
 * Tasks which fork async subtasks will automatically pass on their current context. Therefore essential information
 * can be passed along, without having to provide a method parameter for each value. Since sub-contexts can be of any
 * type, this concept can be enhanced by additional frameworks or application programs.
 * </p>
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/08
 */
@ParametersAreNonnullByDefault
public class CallContext {

    /**
     * Name of the flow variable in the MDC.
     */
    public static final String MDC_FLOW = "flow";
    private static ThreadLocal<CallContext> currentContext = new ThreadLocal<CallContext>();
    private static String nodeName;
    private static Counter interactionCounter = new Counter();

    /**
     * Returns the name of this computation node.
     * <p>
     * This is either the current host name or can be set via <tt>sirius.nodeName</tt>.
     * </p>
     *
     * @return the name of this computation node.
     */
    public static String getNodeName() {
        if (nodeName == null) {
            if (Sirius.getConfig() == null) {
                return "booting";
            }
            nodeName = Sirius.getConfig().getString("sirius.nodeName");
            if (Strings.isEmpty(nodeName)) {
                try {
                    nodeName = InetAddress.getLocalHost().getHostName();
                } catch (UnknownHostException e) {
                    Async.LOG.WARN(Strings.apply("Cannot determine hostname - consider setting 'sirius.nodeName' in the configuration."));
                    nodeName = "unknown";
                }
            }
        }

        return nodeName;
    }

    /**
     * Returns the context for the current thread.
     *
     * @return the <tt>CallContext</tt> of the current thread.
     */
    @Nonnull
    public static CallContext getCurrent() {
        CallContext result = currentContext.get();
        if (result == null) {
            return initialize();
        }

        return result;
    }

    /*
     * Initializes a new context, either with a new flow-id or with the given one.
     */
    private static CallContext initialize(String externalFlowId) {
        CallContext ctx = new CallContext();
        ctx.addToMDC(MDC_FLOW, externalFlowId);
        ctx.setLang(NLS.getDefaultLanguage());
        currentContext.set(ctx);
        interactionCounter.inc();
        return ctx;
    }

    /**
     * Provides access to the interaction counter.
     * <p>
     * This counts all CallContexts which have been created and is used
     * to provide rough system utilization metrics.
     * </p>
     *
     * @return the Counter, which contains the total number of CallContexts created
     */
    public static Counter getInteractionCounter() {
        return interactionCounter;
    }

    /**
     * Creates a new CallContext for the given thread.
     * <p>
     * Discards the current <tt>CallContext</tt>, if there was already one.
     * </p>
     *
     * @return the newly created CallContext, which is already attached to the current thread.
     */
    public static CallContext initialize() {
        return initialize(getNodeName() + "/" + interactionCounter.getCount());
    }

    /**
     * Sets the CallContext for the current thread.
     *
     * @param context the context to use for the current thread.
     */
    public static void setCurrent(CallContext context) {
        currentContext.set(context);
    }

    private Map<String, String> mdc = new LinkedHashMap<String, String>();
    private Map<Class<?>, Object> subContext = new HashMap<Class<?>, Object>();
    private Watch watch = Watch.start();
    private String lang = NLS.getDefaultLanguage();

    /**
     * Returns the current mapped diagnostic context (MDC).
     *
     * @return a list of name-value pair representing the current mdc.
     */
    public List<Tuple<String, String>> getMDC() {
        return Tuple.fromMap(mdc);
    }

    /**
     * Returns the value of the named variable in the mdc.
     *
     * @param key the name of the variable to read.
     * @return the value of the mapped diagnostic context.
     */
    public Value getMDCValue(String key) {
        return Value.of(mdc.get(key));
    }

    /**
     * Returns the Watch representing the execution time.
     *
     * @return a Watch, representing the duration since the creation of the <b>CallContext</b>. Due to CallContexts
     *         being passed to forked sub tasks, the returned duration can be longer than the execution time within the
     *         current thread.
     */
    public Watch getWatch() {
        return watch;
    }

    /**
     * Adds a value to the mapped diagnostic context.
     *
     * @param key   the name of the value to add
     * @param value the value to add to the mdc.
     */
    public void addToMDC(String key, @Nullable String value) {
        mdc.put(key, value);
    }

    /**
     * Removes the value of the mdc for key.
     *
     * @param key the name of the value to remove.
     */
    public void removeFromMDC(String key) {
        mdc.remove(key);
    }

    /**
     * Returns or creates the sub context of the given type.
     * <p>
     * The class of the sub context must provide a no-args constructor, as it will be instantiated if non existed.
     * </p>
     *
     * @param contextType the type of the sub-context to be returned.
     * @return an instance of the given type. If no instance was available, a new one is created
     */
    @Nonnull
    public <C> C get(Class<C> contextType) {
        try {
            Object result = subContext.get(contextType);
            if (result == null) {
                result = contextType.newInstance();
                subContext.put(contextType, result);
            }

            return (C) result;
        } catch (Throwable e) {
            throw Exceptions.handle()
                    .error(e)
                    .withSystemErrorMessage("Cannot get instance of %s from current CallContext: %s (%s)",
                            contextType.getName())
                    .handle();
        }
    }

    /**
     * Used to apply the MDC to the context used by Log4J. This is automatically called by
     * {@link sirius.kernel.health.Log}.
     */
    public void applyToLog4j() {
        Hashtable<String, String> ctx = MDC.getContext();
        if (ctx == null) {
            for (Map.Entry<String, String> e : mdc.entrySet()) {
                MDC.put(e.getKey(), e.getValue());
            }
        } else {
            ctx.clear();
            ctx.putAll(mdc);
        }
    }

    /**
     * Returns the current language determined for the current thread.
     *
     * @return a two-letter language code used for the current thread.
     */
    public String getLang() {
        return lang;
    }

    /**
     * Sets the current language for the current thread.
     *
     * @param lang the two-letter language code for this thread.
     */
    public void setLang(String lang) {
        this.lang = lang;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : mdc.entrySet()) {
            sb.append(e.getKey());
            sb.append(": ");
            sb.append(e.getValue());
            sb.append("\n");
        }

        return sb.toString();
    }
}
