/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.web.jdbc;

import com.google.common.collect.Maps;
import sirius.kernel.commons.Value;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A small wrapper class to represent a result row.
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/11
 */
public class Row {
    protected Map<String, Object> fields = Maps.newLinkedHashMap();

    /**
     * Returns all stored fields as map.
     *
     * @return the underlying map, containing all fields and values. Modifying this map will modify the row
     */
    public Map<String, Object> getFields() {
        return fields;
    }

    /**
     * Returns the value associated with the given key.
     *
     * @param key the name of the field to retrieve
     * @return the value associated with the given key wrapped as {@link sirius.kernel.commons.Value}
     */
    public Value getValue(Object key) {
        return Value.of(fields.get(key));
    }

    /**
     * Returns a sub list which was stored for a given key.
     * <p>
     * It is assumed that the object associated with the given key is a <code>List&lt;Row&gt;</code>.
     * </p>
     *
     * @param key the name of the field to retrieve
     * @return the list associated with the given key
     * @throws ClassCastException if the stored value isn't a list. <tt>null</tt> is handled gracefully
     */
    @SuppressWarnings("unchecked")
    public List<Row> getSublist(Object key) {
        return (List<Row>) getValue(key).get(Collections.emptyList());
    }

    /**
     * Stores a value for the given key.
     * <p>
     * Can be used to add computed values for further processing.
     * </p>
     *
     * @param key   the key to bind the value to
     * @param value the value to be stored
     */
    public void setValue(String key, Object value) {
        fields.put(key, value);
    }

    @Override
    public String toString() {
        return "Row [" + fields + "]";
    }

}
