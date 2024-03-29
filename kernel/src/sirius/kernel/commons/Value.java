/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons;

import org.joda.time.DateMidnight;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import sirius.kernel.nls.NLS;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Pattern;

/**
 * Provides a generic wrapper for a value which is read from an untyped context
 * like HTTP parameters.
 * <p>
 * It supports elegant <code>null</code> handling and type
 * conversions.
 * </p>
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/08
 */
public class Value {

    private Object data;

    /**
     * Use <code>Amount.of</code> to create a new instance.
     */
    private Value() {
        super();
    }

    /**
     * Creates a new wrapper for the given data.
     */
    public static Value of(Object data) {
        Value val = new Value();
        val.data = data;
        return val;
    }

    /**
     * Determines if the wrapped value is <tt>null</tt>
     *
     * @return <tt>true</tt> if the wrapped value is null, <tt>false</tt> otherwise
     */
    public boolean isNull() {
        return data == null;
    }

    /**
     * Determines if the wrapped value is an empty string.
     *
     * @return <tt>true</tt> if the wrapped value is an empty string, <tt>false</tt> otherwise
     */
    public boolean isEmptyString() {
        return Strings.isEmpty(data);
    }

    /**
     * Checks if the given value is filled and contains the given needle in its
     * string representation
     *
     * @param needle the substring to search
     * @return <tt>true</tt> if the given substring <tt>needle</tt> was found in the wrapped objects string
     *         representation, <tt>false</tt> otherwise
     */
    public boolean contains(String needle) {
        return asString("").contains(needle);
    }

    /**
     * Determines if the wrapped value is not null.
     *
     * @return <tt>true</tt> if the wrapped value is neither <tt>null</tt> nor ""
     */
    public boolean isFilled() {
        return !isEmptyString();
    }

    /**
     * Returns a new {@link Value} which will be empty its value equals one of the given ignored values.
     *
     * @return a <tt>Value</tt> which is empty if the currently wrapped value equals to one of the given values.
     *         Otherwise the current value is returned.
     */
    public Value ignore(String... ignoredValues) {
        if (isEmptyString()) {
            return this;
        }
        for (String val : ignoredValues) {
            if (data.equals(val)) {
                return Value.of(null);
            }
        }
        return this;
    }

    /**
     * Returns a new <tt>Value</tt> which will wrap the given value, if the current value is empty.
     * Otherwise, the current value will be returned.
     *
     * @return a new Value wrapping the given value or the current value if this is not empty.
     */
    public Value replaceEmptyWith(Object value) {
        if (isFilled()) {
            return this;
        }
        data = value;
        return this;
    }

    /**
     * Returns a value which wraps <code>this + separator + value</code>
     * <p>
     * If the current value is empty, the given value is returned (without the separator). If the given
     * value is an empty string, the current value is returned (without the separator).
     * </p>
     *
     * @param separator the separator to be put in between the two. If the given value is <tt>null</tt>, "" is assumed
     * @param value     the value to be appended to the current value.
     * @return a <tt>Value</tt> representing the current value appended with the given value and separated
     *         with the given separator
     */
    public Value append(String separator, Object value) {
        if (Strings.isEmpty(value)) {
            return this;
        }
        if (isEmptyString()) {
            return Value.of(value);
        }
        if (separator == null) {
            separator = "";
        }
        return Value.of(toString() + separator + value.toString());
    }

    /**
     * Returns a value which wraps <code>value + separator + this</code>
     * <p>
     * If the current value is empty, the given value is returned (without the separator). If the given
     * value is an empty string, the current value is returned (without the separator).
     * </p>
     *
     * @param separator the separator to be put in between the two. If the given value is <tt>null</tt>, "" is assumed
     * @param value     the value to be appended to the current value.
     * @return a <tt>Value</tt> representing the given value appended with the current value and separated
     *         with the given separator
     */
    public Value prepend(String separator, Object value) {
        if (Strings.isEmpty(value)) {
            return this;
        }
        if (isEmptyString()) {
            return Value.of(value);
        }
        if (separator == null) {
            separator = "";
        }
        return Value.of(value.toString() + separator + toString());
    }

    /**
     * Cuts and returns the first n given characters of the string representation of this value.
     * <p>
     * <b>Note:</b> This modifies the internal state of this value, since the number of characters is cut from
     * the string representation of the current object and the remaining string is stored as new internal value.
     * </p>
     * <p>
     * If the wrapped value is empty, "" is returned. If the string representation of the wrapped object
     * is shorter than maxNumberOfCharacters, the remaining string is returned and the internal value is set to
     * <tt>null</tt>.
     * </p>
     * <p>
     * This can be used to cut a string into sub strings of a given length:
     * <code>
     * <pre>
     *             Value v = Value.of("This is a long string...");
     *             while(v.isFilled()) {
     *                 System.out.println("Up to 5 chars of v: "+v.eat(5));
     *             }
     *         </pre>
     * </code>
     * </p>
     *
     * @param maxNumberOfCharacters the max length of the string to cut from the wrapped value
     * @return the first maxNumberOfCharacters of the wrapped values string representation, or less if it is shorter.
     *         Returns "" if the wrapped value is empty.
     */
    public String eat(int maxNumberOfCharacters) {
        if (isEmptyString()) {
            return "";
        }
        String value = asString();
        if (value.length() < maxNumberOfCharacters) {
            data = null;
            return value;
        }
        data = value.substring(maxNumberOfCharacters);
        return value.substring(0, maxNumberOfCharacters);
    }

    private static final Pattern NUMBER = Pattern.compile("\\d+(\\.\\d+)?");

    /**
     * Checks if the current value is numeric (integer or double).
     *
     * @return <tt>true</tt> if the wrapped value is either a {@link Number} or an {@link Amount} or
     *         if it is a string which can be converted to a long or double
     */
    public boolean isNumeric() {
        return data != null && (data instanceof Number ||
                data instanceof Amount || NUMBER.matcher(asString("")).matches());
    }

    /**
     * Returns the wrapped object
     *
     * @return the wrapped object of this <tt>Value</tt>
     */
    @Nullable
    public Object get() {
        return data;
    }

    /**
     * Returns the internal data or the given <tt>defaultValue</tt>
     *
     * @return the wrapped value or the given defaultValue if the wrapped value is <tt>null</tt>
     */
    @Nonnull
    public Object get(@Nonnull Object defaultValue) {
        return data == null ? defaultValue : data;
    }

    /**
     * Converts or casts the wrapped object to the given <tt>targetClazz</tt>
     *
     * @param targetClazz  the desired class to which the wrapped value should be converted or casted.
     * @param defaultValue the default value if the wrapped object is empty or cannot be cast to the given target.
     * @return a converted instance of type targetClass or the defaultValue if no conversion was possible
     * @throws IllegalArgumentException if the given <tt>targetClazz</tt> is unknown
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T> T coerce(Class<T> targetClazz, T defaultValue) {
        if (boolean.class.equals(targetClazz) && defaultValue == null) {
            if (Strings.isEmpty(data)) {
                return (T) Boolean.FALSE;
            }
            return (T) (Boolean) Boolean.parseBoolean(String.valueOf(data));
        }
        if (data == null) {
            return defaultValue;
        }
        if (targetClazz.isAssignableFrom(data.getClass())) {
            return (T) data;
        }
        if (String.class.equals(targetClazz)) {
            return (T) NLS.toMachineString(data);
        }
        if (BigDecimal.class.equals(targetClazz)) {
            return (T) getBigDecimal(null);
        }
        if (Amount.class.equals(targetClazz)) {
            return (T) getAmount();
        }
        if (Date.class.equals(targetClazz) && data instanceof Calendar) {
            return (T) ((Calendar) data).getTime();
        }
        if (Calendar.class.equals(targetClazz) && data instanceof Date) {
            Calendar cal = Calendar.getInstance();
            cal.setTime((Date) data);
            return (T) cal;
        }
        if (DateTime.class.equals(targetClazz) && (data instanceof Date || data instanceof Calendar)) {
            return (T) new DateTime(data);
        }
        if (DateTime.class.equals(targetClazz) && data instanceof LocalDate) {
            return (T) ((LocalDate) data).toDateMidnight().toDateTime();
        }
        if (DateTime.class.equals(targetClazz) && data instanceof DateMidnight) {
            return (T) ((DateMidnight) data).toDateTime();
        }
        if (LocalDate.class.equals(targetClazz) && (data instanceof Date || data instanceof Calendar)) {
            return (T) new LocalDate(data);
        }
        if (LocalDate.class.equals(targetClazz) && data instanceof DateTime) {
            return (T) ((DateTime) data).toLocalDate();
        }
        if (LocalDate.class.equals(targetClazz) && data instanceof DateMidnight) {
            return (T) ((DateMidnight) data).toLocalDate();
        }
        if (LocalTime.class.equals(targetClazz) && (data instanceof Date || data instanceof Calendar)) {
            return (T) new LocalTime(data);
        }
        if (LocalTime.class.equals(targetClazz) && data instanceof LocalDate) {
            return (T) ((LocalDate) data).toDateMidnight().toDateTime().toLocalTime();
        }
        if (LocalTime.class.equals(targetClazz) && data instanceof DateMidnight) {
            return (T) ((DateMidnight) data).toDateTime().toLocalTime();
        }
        if (LocalTime.class.equals(targetClazz) && data instanceof DateTime) {
            return (T) ((DateTime) data).toLocalTime();
        }
        if (targetClazz.isEnum()) {
            try {
                if (Strings.isEmpty(asString(""))) {
                    return defaultValue;
                }
                return (T) Enum.valueOf((Class<Enum>) targetClazz, asString(""));
            } catch (Exception e) {
                return (T) Enum.valueOf((Class<Enum>) targetClazz, asString("").toUpperCase());
            }
        }
        if (data instanceof String) {
            try {
                return (T) NLS.parseMachineString(targetClazz, data.toString().trim());
            } catch (Throwable e) {
                return defaultValue;
            }
        }
        if (Integer.class.equals(targetClazz) || int.class.equals(targetClazz)) {
            if (data instanceof Double) {
                return (T) (Integer) ((Long) Math.round((Double) data)).intValue();
            }
            return (T) getInteger();
        }
        if (Long.class.equals(targetClazz) || long.class.equals(targetClazz)) {
            if (data instanceof Double) {
                return (T) (Long) Math.round((Double) data);
            }
            return (T) getLong();
        }

        throw new IllegalArgumentException(Strings.apply("Cannot convert '%s' to target class: %s ",
                                                         data,
                                                         targetClazz));
    }

    /**
     * Returns the wrapped value if it is an instance of the given clazz or the <tt>defaultValue</tt> otherwise.
     *
     * @param clazz        the desired class of the return type
     * @param defaultValue the value which is returned if the wrapped value is not assignable to the given class.
     * @return the wrapped value if the given <tt>clazz</tt> is assignable from wrapped values class
     *         or the <tt>defaultValue</tt> otherwise
     */
    @SuppressWarnings("unchecked")
    public <V> V get(Class<V> clazz, V defaultValue) {
        Object result = get(defaultValue);
        if (result == null || !clazz.isAssignableFrom(result.getClass())) {
            return defaultValue;
        }
        return (V) result;
    }

    /**
     * Returns the data converted to a string, or <tt>null</tt> if the wrapped value is null
     * <p>
     * The conversion method used is {@link NLS#toMachineString(Object)}
     * </p>
     *
     * @return a string representation of the wrapped object or <tt>null</tt> if the wrapped value is <tt>null</tt>
     */
    @Nullable
    public String getString() {
        return isNull() ? null : NLS.toMachineString(data);
    }

    /**
     * Returns the wrapped data converted to a string or <tt>defaultValue</tt> if the wrapped value is <tt>null</tt>
     * <p>
     * The conversion method used is {@link NLS#toMachineString(Object)}
     * </p>
     *
     * @return a string representation of the wrapped object
     *         or <tt>defaultValue</tt> if the wrapped value is <tt>null</tt>
     */
    @Nonnull
    public String asString(@Nonnull String defaultValue) {
        return isNull() ? defaultValue : NLS.toMachineString(data);
    }

    /**
     * Returns the wrapped data converted to a string or <tt>""</tt> if the wrapped value is <tt>null</tt>
     * <p>
     * The conversion method used is {@link NLS#toMachineString(Object)}
     * </p>
     *
     * @return a string representation of the wrapped object or <tt>""</tt> if the wrapped value is <tt>null</tt>
     */
    @Nonnull
    public String asString() {
        return NLS.toMachineString(data);
    }

    @Override
    public String toString() {
        return asString();
    }

    /**
     * Returns the wrapped data converted to a string like {@link #asString()}
     * while "smart rounding" ({@link NLS#smartRound(double)} <tt>Double</tt> and <tt>BigDecimal</tt> values.
     * <p>
     * This method behaves just like <tt>asString</tt>, except for <tt>Double</tt> and <tt>BigDecimal</tt> values
     * where the output is "smart rounded". Therefore, 12.34 will be formatted as <code>12.34</code> but 1.000 will
     * be formatted as <code>1</code>
     * </p>
     *
     * @return a string representation of the wrapped object as generated by <tt>asString</tt>
     *         except for <tt>Double</tt> or <tt>BigDecimal</tt> values, which are "smart rounded".
     * @see NLS#smartRound(double)
     */
    @Nonnull
    public String asSmartRoundedString() {
        if (data == null) {
            return "";
        }
        if (data instanceof Double) {
            return NLS.smartRound((Double) data);
        }
        if (data instanceof BigDecimal) {
            return NLS.smartRound(((BigDecimal) data).doubleValue());
        }
        return asString();
    }


    /**
     * Converts the wrapped value to a <tt>boolean</tt> or returns the given <tt>defaultValue</tt>
     * if no conversion is possible.
     * <p>
     * To convert a value, {@link Boolean#parseBoolean(String)} is used, where <code>toString</code> is called on all
     * non-string objects.
     * </p>
     *
     * @param defaultValue the value to be used if the wrapped value cannot be converted to a boolean.
     * @return <tt>true</tt> if the wrapped value is <tt>true</tt>
     *         or if the string representation of it is <code>"true"</code>. Returns <tt>false</tt> otherwise,
     *         especially if the wrapped value is <tt>null</tt>
     */
    public boolean asBoolean(boolean defaultValue) {
        if (isNull()) {
            return defaultValue;
        }
        if (data instanceof Boolean) {
            return (Boolean) data;
        }
        return Boolean.parseBoolean(String.valueOf(data));
    }

    /**
     * Boilerplate method for <code>asBoolean(false)</code>
     *
     * @return <tt>true</tt> if the wrapped value is <tt>true</tt>
     *         or if the string representation of it is <code>"true"</code>. Returns <tt>false</tt> otherwise,
     *         especially if the wrapped value is <tt>null</tt>
     */
    public boolean asBoolean() {
        return asBoolean(false);
    }

    /**
     * Returns the int value for the wrapped value or <tt>defaultValue</tt> if the wrapped value isn't an integer and
     * cannot be converted to one.
     * <p>
     * If the wrapped value is an <tt>Integer</tt> or <tt>BigDecimal</tt>, it is either directly returned or converted
     * by calling {@link java.math.BigDecimal#longValue()}.
     * </p>
     * <p>
     * Otherwise {@link Integer#parseInt(String)} is called on the string representation of the wrapped value. If
     * parsing fails, or if the wrapped value was <tt>null</tt>, the <tt>defaultValue</tt> will be returned.
     * </p>
     *
     * @param defaultValue the value to be used, if no conversion to <tt>int</tt> is possible.
     * @return the wrapped value casted or converted to <tt>int</tt> or <tt>defaultValue</tt>
     *         if no conversion is possible.
     */
    public int asInt(int defaultValue) {
        try {
            if (isNull()) {
                return defaultValue;
            }
            if (data instanceof Integer) {
                return (Integer) data;
            }
            if (data instanceof BigDecimal) {
                return (int) ((BigDecimal) data).longValue();
            }

            return Integer.parseInt(String.valueOf(data));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Returns the int value for the wrapped value or <tt>null</tt> if the wrapped value isn't an integer and
     * cannot be converted to one.
     * <p>
     * If the wrapped value is an <tt>Integer</tt> or <tt>BigDecimal</tt>, it is either directly returned or converted
     * by calling {@link java.math.BigDecimal#longValue()}.
     * </p>
     * <p>
     * Otherwise {@link Integer#parseInt(String)} is called on the string representation of the wrapped value. If
     * parsing fails, or if the wrapped value was <tt>null</tt>, <tt>null</tt> will be returned.
     * </p>
     *
     * @return the wrapped value casted or converted to <tt>Integer</tt> or <tt>null</tt>
     *         if no conversion is possible.
     */
    @Nullable
    public Integer getInteger() {
        try {
            if (isNull()) {
                return null;
            }
            if (data instanceof Integer) {
                return (Integer) data;
            }
            if (data instanceof BigDecimal) {
                return (int) ((BigDecimal) data).longValue();
            }
            return Integer.parseInt(String.valueOf(data));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Returns the long value for the wrapped value or <tt>defaultValue</tt> if the wrapped value isn't a long and
     * cannot be converted to one.
     * <p>
     * If the wrapped value is a <tt>Long</tt>, <tt>Integer</tt> or <tt>BigDecimal</tt>,
     * it is either directly returned or converted by calling {@link java.math.BigDecimal#longValue()}.
     * </p>
     * <p>
     * Otherwise {@link Long#parseLong(String)} is called on the string representation of the wrapped value. If
     * parsing fails, or if the wrapped value was <tt>null</tt>, the <tt>defaultValue</tt> will be returned.
     * </p>
     *
     * @param defaultValue the value to be used, if no conversion to <tt>long</tt> is possible.
     * @return the wrapped value casted or converted to <tt>long</tt> or <tt>defaultValue</tt>
     *         if no conversion is possible.
     */
    public long asLong(long defaultValue) {
        try {
            if (isNull()) {
                return defaultValue;
            }
            if (data instanceof Long) {
                return (Long) data;
            }
            if (data instanceof Integer) {
                return (Integer) data;
            }
            if (data instanceof BigDecimal) {
                return ((BigDecimal) data).longValue();
            }
            return Long.parseLong(String.valueOf(data));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Returns the long value for the wrapped value or <tt>null</tt> if the wrapped value isn't a long and
     * cannot be converted to one.
     * <p>
     * If the wrapped value is a <tt>Long</tt>, <tt>Integer</tt> or <tt>BigDecimal</tt>, it is either directly
     * returned or by calling {@link java.math.BigDecimal#longValue()}.
     * </p>
     * <p>
     * Otherwise {@link Long#parseLong(String)} is called on the string representation of the wrapped value. If
     * parsing fails, or if the wrapped value was <tt>null</tt>, <tt>null</tt> will be returned.
     * </p>
     *
     * @return the wrapped value casted or converted to <tt>Long</tt> or <tt>null</tt>
     *         if no conversion is possible.
     */
    @Nullable
    public Long getLong() {
        try {
            if (isNull()) {
                return null;
            }
            if (data instanceof Long) {
                return (Long) data;
            }
            return Long.parseLong(String.valueOf(data));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Returns the double value for the wrapped value or <tt>defaultValue</tt> if the wrapped value isn't a double and
     * cannot be converted to one.
     * <p>
     * If the wrapped value is a <tt>Double</tt>, <tt>Long</tt>, <tt>Integer</tt> or <tt>BigDecimal</tt>,
     * it is either directly returned or converted by calling {@link java.math.BigDecimal#doubleValue()}.
     * </p>
     * <p>
     * Otherwise {@link Double#parseDouble(String)} is called on the string representation of the wrapped value. If
     * parsing fails, or if the wrapped value was <tt>null</tt>, the <tt>defaultValue</tt> will be returned.
     * </p>
     *
     * @param defaultValue the value to be used, if no conversion to <tt>double</tt> is possible.
     * @return the wrapped value casted or converted to <tt>double</tt> or <tt>defaultValue</tt>
     *         if no conversion is possible.
     */
    public double asDouble(double defaultValue) {
        try {
            if (isNull()) {
                return defaultValue;
            }
            if (data instanceof Double) {
                return (Double) data;
            }
            if (data instanceof Long) {
                return (Long) data;
            }
            if (data instanceof Integer) {
                return (Integer) data;
            }
            if (data instanceof BigDecimal) {
                return ((BigDecimal) data).doubleValue();
            }
            return Double.parseDouble(String.valueOf(data));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Returns the <tt>BigDecimal</tt> value for the wrapped value or <tt>defaultValue</tt> if the wrapped value
     * isn't a BigDecimal and cannot be converted to one.
     * <p>
     * If the wrapped value is a <tt>BigDecimal</tt>, tt>Double</tt>, <tt>Long</tt> or <tt>Integer</tt>,
     * it is either directly returned or converted by calling <tt>java.math.BigDecimal#valueOf</tt>.
     * </p>
     * <p>
     * Otherwise {@link BigDecimal#BigDecimal(String, java.math.MathContext)} is called on the string representation
     * of the wrapped value (with "," replaced to ".") and <tt>MathContext.UNLIMITED</tt>. If parsing fails, or if
     * the wrapped value was <tt>null</tt>, the <tt>defaultValue</tt> will be returned.
     * </p>
     *
     * @param defaultValue the value to be used, if no conversion to <tt>BigDecimal</tt> is possible.
     * @return the wrapped value casted or converted to <tt>BigDecimal</tt> or <tt>defaultValue</tt>
     *         if no conversion is possible.
     */
    @Nonnull
    public BigDecimal getBigDecimal(@Nonnull BigDecimal defaultValue) {
        try {
            if (isNull()) {
                return defaultValue;
            }
            if (data instanceof BigDecimal) {
                return (BigDecimal) data;
            }
            if (data instanceof Double) {
                return BigDecimal.valueOf((Double) data);
            }
            if (data instanceof Long) {
                return BigDecimal.valueOf((Long) data);
            }
            if (data instanceof Integer) {
                return BigDecimal.valueOf((Integer) data);
            }
            return new BigDecimal(asString("").replace(",", "."), MathContext.UNLIMITED);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Returns the <tt>Amount</tt> for the wrapped value.
     * <p>
     * If the wrapped value can be converted to a BigDecimal ({@link #getBigDecimal(java.math.BigDecimal)},
     * an <tt>Amount</tt> for the result is returned. Otherwise an empty <tt>Amount</tt> is returned.
     * </p>
     *
     * @return the wrapped value converted to <tt>Amount</tt>. The result might be an empty amount, if the wrapped
     *         value is <tt>null</tt> or if no conversion was possible.
     * @see #getBigDecimal(java.math.BigDecimal)
     */
    public Amount getAmount() {
        return Amount.of(getBigDecimal(null));
    }

    /**
     * Converts the wrapped value to an enum constant of the given <tt>clazz</tt>.
     *
     * @param clazz the type of the enum to use
     * @return an enum constant of the given <tt>clazz</tt> with the same name as the wrapped value
     *         or <tt>null</tt> if no matching constant was found
     */
    @SuppressWarnings("unchecked")
    public <E extends Enum<E>> E asEnum(Class<E> clazz) {
        if (data == null) {
            return null;
        }
        if (clazz.isAssignableFrom(data.getClass())) {
            return (E) data;
        }
        try {
            return Enum.valueOf(clazz, String.valueOf(data));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Checks if the string representation of the wrapped value starts with the given string.
     *
     * @param value the substring with which the string representation must start
     * @return <tt>true</tt> if the string representation starts with <tt>value</tt>, <tt>false</tt> otherwise.
     *         If the current value is empty, it is treated as ""
     */
    public boolean startsWith(String value) {
        return asString().startsWith(value);
    }

    /**
     * Checks if the string representation of the wrapped value ends with the given string.
     *
     * @param value the substring with which the string representation must end
     * @return <tt>true</tt> if the string representation ends with <tt>value</tt>, <tt>false</tt> otherwise.
     *         If the current value is empty, it is treated as ""
     */
    public boolean endsWith(String value) {
        return asString().endsWith(value);
    }

    /**
     * Returns a trimmed version of the string representation of the wrapped value.
     * <p>
     * The conversion method used is {@link #asString()}, therefore an empty value will yield <code>""</code>.
     * </p>
     *
     * @return a string representing the wrapped value without leading or trailing spaces.
     */
    @Nonnull
    public String trim() {
        return asString().trim();
    }

    /**
     * Returns the first N (<tt>length</tt>) characters of the string representation of the wrapped value.
     * <p>
     * If the wrapped value is <tt>null</tt>, <code>""</code> will be returned. If the string representation is
     * shorter than <tt>length</tt>, the whole string is returned.
     * </p>
     * <p>
     * If <tt>length</tt> is negative, the string representation <b>without</b> the first N (<tt>length</tt>)
     * characters is returned. If the string representation is too short, <code>""</code> is returned.
     * </p>
     *
     * @param length the number of characters to return or to omit (if <tt>length</tt> is negative)
     * @return the first N characters (or less if the string representation of the wrapped value is shorter)
     *         or the string representation without the first N characters (or "" if the representation is too short)
     *         if <tt>length is negative</tt>. Returns <code>""</code> if the wrapped value is <tt>null</tt>
     */
    @Nonnull
    public String left(int length) {
        if (isNull()) {
            return "";
        }
        String value = asString();
        if (length < 0) {
            length = length * -1;
            if (value.length() < length) {
                return "";
            }
            return value.substring(length);
        } else {
            if (value.length() < length) {
                return value;
            }
            return value.substring(0, length);
        }
    }

    /**
     * Returns the last N (<tt>length</tt>) characters of the string representation of the wrapped value.
     * <p>
     * If the wrapped value is <tt>null</tt>, <code>""</code> will be returned. If the string representation is
     * shorter than <tt>length</tt>, the whole string is returned.
     * </p>
     * <p>
     * If <tt>length</tt> is negative, the string representation <b>without</b> the last N (<tt>length</tt>)
     * characters is returned. If the string representation is too short, <code>""</code> is returned.
     * </p>
     *
     * @param length the number of characters to return or to omit (if <tt>length</tt> is negative)
     * @return the last N characters (or less if the string representation of the wrapped value is shorter)
     *         or the string representation without the last N characters (or "" if the representation is too short)
     *         if <tt>length is negative</tt>. Returns <code>""</code> if the wrapped value is <tt>null</tt>
     */
    @Nonnull
    public String right(int length) {
        if (isNull()) {
            return "";
        }
        String value = asString();
        if (length < 0) {
            length = length * -1;
            if (value.length() < length) {
                return value;
            }
            return value.substring(0, value.length() - length);
        } else {
            if (value.length() < length) {
                return value;
            }
            return value.substring(value.length() - length);
        }
    }

    /**
     * Returns a substring of the string representation of the wrapped value.
     * <p>
     * Returns the substring starting at <tt>startIndex</tt> and ending at <tt>endIndex</tt>. If the given
     * end index is greater than the string length, the complete substring from <tt>startIndex</tt> to the end of
     * the string is returned. If the <tt>startIndex</tt> is greater than the string length, <code>""</code> is
     * returned.
     * </p>
     *
     * @return a substring like {@link String#substring(int, int)} or <code>""</code> if the wrapped value
     */
    @Nonnull
    public String substring(int startIndex, int endIndex) {
        if (isNull()) {
            return "";
        }
        String value = asString();
        if (startIndex > value.length()) {
            return "";
        }
        return value.substring(startIndex, Math.min(value.length(), endIndex));
    }

    /**
     * Returns the length of the string representation of the wrapped value.
     *
     * @return the length of the string representation of the wrapped value or 0, if the wrapped value is <tt>null</tt>
     */
    public int length() {
        if (isNull()) {
            return 0;
        }
        return asString().length();
    }

    /**
     * Returns an uppercase version of the string representation of the wrapped value.
     *
     * @return an uppercase version of the string representation of the wrapped value or <code>""</code> if the
     *         wrapped value is <tt>null</tt>
     */
    @Nonnull
    public String toUpperCase() {
        if (isNull()) {
            return "";
        }
        return asString().toUpperCase();
    }

    /**
     * Returns an lowercase version of the string representation of the wrapped value.
     *
     * @return an lowercase version of the string representation of the wrapped value or <code>""</code> if the
     *         wrapped value is <tt>null</tt>
     */
    @Nonnull
    public String toLowerCase() {
        if (isNull()) {
            return "";
        }
        return asString().toLowerCase();
    }

    /**
     * Checks if the value implements the given class.
     *
     * @param clazz the class to check
     * @return <tt>true</tt> if the wrapped value is assignable to the given <tt>clazz</tt>
     */
    public boolean is(Class<?> clazz) {
        return get() != null && clazz.isAssignableFrom(get().getClass());
    }

    /**
     * Replaces the given <tt>pattern</tt> with the given replacement in the string representation
     * of the wrapped object
     *
     * @param pattern     the pattern to replace
     * @param replacement the replacement to be used for <tt>pattern</tt>
     * @return a <tt>Value</tt> where all occurrences of pattern in the string <tt>representation</tt> of the
     *         wrapped value are replaced by <tt>replacement</tt>. If the wrapped value is null, <tt>this</tt>
     *         is returned.
     */
    @Nonnull
    public Value replace(String pattern, String replacement) {
        if (data != null) {
            return Value.of(data.toString().replace(pattern, replacement));
        }
        return this;
    }

    /**
     * Replaces the given regular expression <tt>pattern</tt> with the given replacement in the string representation
     * of the wrapped object
     *
     * @param pattern     the regular expression to replace
     * @param replacement the replacement to be used for <tt>pattern</tt>
     * @return a <tt>Value</tt> where all occurences of pattern in the string <tt>representation</tt> of the
     *         wrapped value are replaced by <tt>replacement</tt>. If the wrapped value is null, <tt>this</tt>
     *         is returned.
     */
    @Nonnull
    public Value regExReplace(String pattern, String replacement) {
        if (data != null) {
            data = data.toString().replaceAll(pattern, replacement);
        }
        return this;
    }

    @Override
    public boolean equals(Object other) {
        if (data == other) {
            return true;
        }
        if (data == null) {
            return other == null;
        }
        return data.equals(other);
    }

    @Override
    public int hashCode() {
        return data == null ? 0 : data.hashCode();
    }

    /**
     * Returns a <tt>Value</tt> containing a translated value using the string representation
     * of the wrapped value as key.
     *
     * @return a <tt>Value</tt> containing a translated value by calling {@link NLS#get(String)}
     *         if the string representation of the wrapped value starts with <code>$</code>.
     *         The dollar sign is skipped when passing the key to <tt>NLS</tt>. Otherwise <tt>this</tt> is returned.
     * @see NLS#get(String)
     */
    @Nonnull
    @CheckReturnValue
    public Value translate() {
        if (data != null && data instanceof String && ((String) data).startsWith("$")) {
            return Value.of(NLS.get(((String) data).substring(1)));
        }
        return this;
    }
}
