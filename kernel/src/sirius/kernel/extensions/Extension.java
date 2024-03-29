/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.extensions;

import sirius.kernel.commons.Value;

import javax.annotation.Nonnull;

/**
 * Represents an extension loaded via the {@link Extensions} framework.
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/08
 */
public interface Extension {

    /**
     * Returns the type name of the extension
     *
     * @return the name of the type, this extension was registered for
     */
    String getType();

    /**
     * Returns the unique ID of this extension
     *
     * @return the map key, used to register this extension
     */
    String getId();

    /**
     * Returns the complete "dot separated" name like <code>[extension.path].[id]</code>
     *
     * @return the complete access path used to identify this extension
     */
    String getQualifiedName();

    /**
     * Determined if this extension is an artifically created default extension.
     *
     * @return <tt>true</tt> if this is an artifically created default extension used by
     *         {@link Extensions#getExtension(String, String)} if nothing was found
     */
    boolean isDefault();

    /**
     * Returns the {@link Value} defined for the given key.
     * <p>
     * If this extension doesn't provide a value for this key, but there is an extension with the name
     * <tt>default</tt> which provides a value, this is used.
     * </p>
     * <p>
     * If the value in the config file starts with a dollar sign, the value is treated as an i18n key and the
     * returned value will contain the translation for the current language.
     * </p>
     *
     * @param path the access path to retrieve the value
     * @return the value wrapping the contents for the given path. This will never by <tt>null</tt>,
     *         but might be empty: {@link sirius.kernel.commons.Value#isNull()}
     */
    @Nonnull
    Value get(String path);

    /**
     * Returns the duration in milliseconds defined for the given key.
     * <p>
     * If this extension doesn't provide a value for this key, but there is an extension with the name
     * <tt>default</tt> which provides a value, this is used.
     * </p>
     *
     * @param path the access path to retrieve the value
     * @return the encoded duration as milliseconds.
     * @throws sirius.kernel.health.HandledException
     *          if an invalid value was given in the config
     */
    long getMilliseconds(String path);

    /**
     * Returns the {@link Value} defined for the given key or throws a <tt>HandledException</tt> if no value was found
     * <p>
     * If this extension doesn't provide a value for this key, but there is an extension with the name
     * <tt>default</tt> which provides a value, this is used.
     * </p>
     * <p>
     * Returning a {@link Value} instead of a plain object provides lots of conversion methods on the one hand
     * and also guarantees a non null result on the other hand, since a <tt>Value</tt> can be empty.
     * </p>
     *
     * @param path the access path to retrieve the value
     * @return the value wrapping the contents for the given path. This will never by <tt>null</tt>.
     * @throws sirius.kernel.health.HandledException
     *          if no value was found for the given <tt>path</tt>
     */
    @Nonnull
    Value require(String path);

    /**
     * Creates a new instance of the class which is named in <tt>classProperty</tt>
     * <p>
     * Tries to lookup the value for <tt>classProperty</tt>, fetches the corresponding class and creates a new
     * instance for it.
     * </p>
     * <p>
     * Returning a {@link Value} instead of a plain object provides lots of conversion methods on the one hand
     * and also guarantees a non null result on the other hand, since a <tt>Value</tt> can be empty.
     * </p>
     *
     * @param classProperty the property which is used to retrieve the class name
     * @return a new instance of the given class
     * @throws sirius.kernel.health.HandledException
     *          if no valid class was given, or if no instance could be created
     */
    @Nonnull
    Object make(String classProperty);
}
