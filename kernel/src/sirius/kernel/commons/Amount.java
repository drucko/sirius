/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons;

import sirius.kernel.nls.NLS;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;

/**
 * Provides a wrapper around <tt>BigDecimal</tt> to perform "exact" computations on numeric values.
 * <p>
 * Adds some extended computations as well as locale aware formatting options to perform "exact" computations on
 * numeric value. The internal representation is <tt>BigDecimal</tt> and uses MathContext.DECIMAL128 for
 * numerical operations. Also the scale of each value is fixed to 5 decimal places after the comma, since this is
 * enough for most business applications and rounds away any rounding errors introduced by doubles.
 * </p>
 * A textual representation can be created by calling one of the <tt>toString</tt> methods or by supplying
 * a {@link NumberFormat}.
 * <p/>
 * <p>
 * Being able to be <i>empty</i>, this class handles <tt>null</tt> values gracefully, which simplifies many operations.
 * </p>
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @see NumberFormat
 * @see BigDecimal
 * @since 2013/08
 */
@Immutable
public class Amount implements Comparable<Amount> {

    /**
     * Represents an missing number. This is also the result of division by 0 and other forbidden operations.
     */
    public static final Amount NOTHING = new Amount(null);
    /**
     * Representation of 100.00
     */
    public static final Amount ONE_HUNDRED = new Amount(new BigDecimal(100));
    /**
     * Representation of 0.00
     */
    public static final Amount ZERO = new Amount(BigDecimal.ZERO);
    /**
     * Representation of 1.00
     */
    public static final Amount ONE = new Amount(BigDecimal.ONE);
    /**
     * Representation of 10.00
     */
    public static final Amount TEN = new Amount(BigDecimal.TEN);
    /**
     * Representation of -1.00
     */
    public static final Amount MINUS_ONE = new Amount(new BigDecimal(-1));
    /**
     * Defines the internal precision used for all computations.
     */
    public static final int SCALE = 5;

    private final BigDecimal amount;

    private Amount(BigDecimal amount) {
        if (amount != null) {
            this.amount = amount.setScale(SCALE, RoundingMode.HALF_UP);
        } else {
            this.amount = null;
        }
    }

    /**
     * Converts the given string into a number. If the string is empty, <tt>NOTHING</tt> is returned.
     * If the string is malformed an exception will be thrown.
     *
     * @param value the string value which should be converted into a numeric value.
     * @return an <tt>Amount</tt> representing the given input. <tt>NOTHING</tt> if the input was empty.
     */
    @Nonnull
    public static Amount ofMachineString(@Nullable String value) {
        if (Strings.isEmpty(value)) {
            return NOTHING;
        }
        return of(NLS.parseMachineString(Double.class, value));
    }

    /**
     * Converts the given string into a number which is formatted according the decimal symbols for the current locale.
     *
     * @param value the string value which should be converted into a numeric value.
     * @return an <code>Amount</code> representing the given input. <code>NOTHING</code> if the input was empty.
     * @see NLS
     */
    @Nonnull
    public static Amount ofUserString(@Nullable String value) {
        if (Strings.isEmpty(value)) {
            return NOTHING;
        }
        return of(NLS.parseUserString(Double.class, value));
    }

    /**
     * Converts the given value into a number.
     *
     * @param amount the value which should be converted into an <tt>Amount</tt>
     * @return an <tt>Amount</tt> representing the given input. <tt>NOTHING</tt> if the input was empty.
     */
    @Nonnull
    public static Amount of(@Nullable BigDecimal amount) {
        if (amount == null) {
            return NOTHING;
        }
        return new Amount(amount);
    }

    /**
     * Converts the given value into a number.
     *
     * @param amount the value which should be converted into an <tt>Amount</tt>
     * @return an <tt>Amount</tt> representing the given input
     */
    @Nonnull
    public static Amount of(int amount) {
        return of(new BigDecimal(amount));
    }

    /**
     * Converts the given value into a number.
     *
     * @param amount the value which should be converted into an <tt>Amount</tt>
     * @return an <tt>Amount</tt> representing the given input
     */
    @Nonnull
    public static Amount of(long amount) {
        return of(new BigDecimal(amount));
    }

    /**
     * Converts the given value into a number.
     *
     * @param amount the value which should be converted into an <tt>Amount</tt>
     * @return an <tt>Amount</tt> representing the given input
     */
    @Nonnull
    public static Amount of(double amount) {
        return of(new BigDecimal(amount));
    }

    /**
     * Converts the given value into a number.
     *
     * @param amount the value which should be converted into an <tt>Amount</tt>
     * @return an <tt>Amount</tt> representing the given input. <tt>NOTHING</tt> if the input was empty.
     */
    @Nonnull
    public static Amount of(@Nullable Integer amount) {
        if (amount == null) {
            return NOTHING;
        }
        return of(new BigDecimal(amount));
    }

    /**
     * Converts the given value into a number.
     *
     * @param amount the value which should be converted into an <tt>Amount</tt>
     * @return an <tt>Amount</tt> representing the given input. <tt>NOTHING</tt> if the input was empty.
     */
    @Nonnull
    public static Amount of(@Nullable Long amount) {
        if (amount == null) {
            return NOTHING;
        }
        return of(new BigDecimal(amount));
    }

    /**
     * Converts the given value into a number.
     *
     * @param amount the value which should be converted into an <tt>Amount</tt>
     * @return an <tt>Amount</tt> representing the given input. <tt>NOTHING</tt> if the input was empty.
     */
    @Nonnull
    public static Amount of(@Nullable Double amount) {
        if (amount == null || Double.isInfinite(amount) || Double.isNaN(amount)) {
            return NOTHING;
        }
        return of(new BigDecimal(amount));
    }

    /**
     * Unwraps the internally used <tt>BigDecimal</tt>. May be <tt>null</tt> if this <tt>Amount</tt> is
     * <tt>NOTHING</tt>.
     *
     * @return the internally used <tt>BigDecimal</tt>
     */
    @Nullable
    public BigDecimal getAmount() {
        return amount;
    }

    /**
     * Checks if this contains no value.
     *
     * @return <tt>true</tt> if the internal value is null, <tt>false</tt> otherwise
     */
    public boolean isEmpty() {
        return amount == null;
    }

    /**
     * Checks if this actual number contains a value or not
     *
     * @return <tt>true</tt> if the internal value is a number, <tt>false</tt> otherwise
     */
    public boolean isFilled() {
        return amount != null;
    }

    /**
     * If this actual number if empty, the given value will be returned. Otherwise this will be returned.
     *
     * @param valueIfNothing the value which is used if there is no internal value
     * @return <tt>this</tt> if there is an internal value, <tt>valueIfNothing</tt> otherwise
     */
    @Nonnull
    public Amount fill(@Nonnull Amount valueIfNothing) {
        if (isEmpty()) {
            return valueIfNothing;
        } else {
            return this;
        }
    }

    /**
     * Increases this number by the given amount in percent. If <tt>increase</tt> is 18 and the value of this is 100,
     * the result would by 118.
     *
     * @param increase the percent value by which the value of this will be increased
     * @return <tt>NOTHING</tt> if this or increase is empty, <code>this * (1 + increase / 100)</code> otherwise
     */
    @Nonnull
    @CheckReturnValue
    public Amount increasePercent(@Nullable Amount increase) {
        return times(ONE.add(increase.asDecimal()));
    }

    /**
     * Decreases this number by the given amount in percent. If <tt>decrease</tt> is 10 and the value of this is 100,
     * the result would by 90.
     *
     * @param decrease the percent value by which the value of this will be decreased
     * @return <tt>NOTHING</tt> if this or increase is empty, <code>this * (1 - increase / 100)</code> otherwise
     */
    @Nonnull
    @CheckReturnValue
    public Amount decreasePercent(@Nullable Amount decrease) {
        return times(ONE.subtract(decrease.asDecimal()));
    }

    /**
     * Used to multiply two percentages, like two discounts as if they where applied after each other.
     * <p>
     * This can be used to compute the effective discount if two discounts like 15% and 5% are applied after
     * each other. The result would be <code>(15 + 5) - (15 * 5 / 100)</code> which is <tt>19,25 %</tt>
     * </p>
     *
     * @param percent the second percent value which would be applied after this percent value.
     * @return the effective percent value after both percentages would have been applied
     *         or <tt>NOTHING</tt> if one of both was empty.
     */
    @Nonnull
    @CheckReturnValue
    public Amount multiplyPercent(@Nullable Amount percent) {
        return add(percent).subtract(this.times(percent).divideBy(ONE_HUNDRED));
    }

    /**
     * Adds the given number to <tt>this</tt>, if <tt>other</tt> is not empty. Otherwise <tt>this</tt> will be returned.
     *
     * @param other the operand to add to this.
     * @return an <tt>Amount</tt> representing the sum of <tt>this</tt> and <tt>other</tt> if both values were filled.
     *         If <tt>other</tt> is empty, <tt>this</tt> is returned. If this is empty, <tt>NOTHING</tt> is returned.
     */
    @Nonnull
    @CheckReturnValue
    public Amount add(@Nullable Amount other) {
        if (other == null || other.isEmpty()) {
            return this;
        }
        if (isEmpty()) {
            return NOTHING;
        }
        return Amount.of(amount.add(other.amount));
    }

    /**
     * Subtracts the given number from <tt>this</tt>, if <tt>other</tt> is not empty. Otherwise <tt>this</tt> will be returned.
     *
     * @param other the operand to subtract from this.
     * @return an <tt>Amount</tt> representing the difference of <tt>this</tt> and <tt>other</tt> if both values were filled.
     *         If <tt>other</tt> is empty, <tt>this</tt> is returned. If this is empty, <tt>NOTHING</tt> is returned.
     */
    @Nonnull
    @CheckReturnValue
    public Amount subtract(@Nullable Amount other) {
        if (other == null || other.isEmpty()) {
            return this;
        }
        if (isEmpty()) {
            return NOTHING;
        }
        return Amount.of(amount.subtract(other.amount));
    }

    /**
     * Multiplies the given number with <tt>this</tt>. If either of both is empty, <tt>NOTHING</tt> will be returned.
     *
     * @param other the operand to multiply with this.
     * @return an <tt>Amount</tt> representing the product of <tt>this</tt> and <tt>other</tt> if both values were filled.
     *         If <tt>other</tt> is empty or if <tt>this</tt> is empty, <tt>NOTHING</tt> is returned.
     */
    @Nonnull
    @CheckReturnValue
    public Amount times(@Nonnull Amount other) {
        if (other == null || other.isEmpty() || isEmpty()) {
            return NOTHING;
        }
        return Amount.of(amount.multiply(other.amount));
    }

    /**
     * Divides <tt>this</tt> by the given number. If either of both is empty, or the given number is zero,
     * <tt>NOTHING</tt> will be returned. The division uses {@link MathContext#DECIMAL128}
     *
     * @param other the operand to divide this by.
     * @return an <tt>Amount</tt> representing the division of <tt>this</tt> by <tt>other</tt> or <tt>NOTHING</tt>
     *         if either of both is empty.
     */
    @Nonnull
    @CheckReturnValue
    public Amount divideBy(@Nullable Amount other) {
        if (other == null || other.isZeroOrNull() || isEmpty()) {
            return NOTHING;
        }
        return Amount.of(amount.divide(other.amount, MathContext.DECIMAL128));
    }

    /**
     * Returns the ratio in percent from <tt>this</tt> to <tt>other</tt>.
     * This is equivalent to <code>this / other * 100</code>
     *
     * @param other the base to compute the percentage from.
     * @return an <tt>Amount</tt> representing the ratio between <tt>this</tt> and <tt>other</tt>
     *         or <tt>NOTHING</tt> if either of both is empty.
     */
    @Nonnull
    @CheckReturnValue
    public Amount percentageOf(@Nullable Amount other) {
        return divideBy(other).toPercent();
    }

    /**
     * Returns the increase in percent of <tt>this</tt> over <tt>other</tt>.
     * This is equivalent to <code>((this / other) - 1) * 100</code>
     *
     * @param other the base to compute the increase from.
     * @return an <tt>Amount</tt> representing the percentage increase between <tt>this</tt> and <tt>other</tt>
     *         or <tt>NOTHING</tt> if either of both is empty.
     */
    @Nonnull
    @CheckReturnValue
    public Amount percentageDifferenceOf(@Nonnull Amount other) {
        return divideBy(other).subtract(ONE).toPercent();
    }

    /**
     * Determines if the value is filled and equal to 0.00.
     *
     * @return <tt>true</tt> if this value is filled and equal to 0.00, <tt>false</tt> otherwise.
     */
    public boolean isZero() {
        return (amount != null && amount.compareTo(BigDecimal.ZERO) == 0);
    }

    /**
     * Determines if the value is filled and not equal to 0.00.
     *
     * @return <tt>true</tt> if this value is filled and not equal to 0.00, <tt>false</tt> otherwise.
     */
    public boolean isNonZero() {
        return (amount != null && amount.compareTo(BigDecimal.ZERO) != 0);
    }

    /**
     * Determines if the value is filled and greater than 0.00
     *
     * @return <tt>true</tt> if this value is filled and greater than 0.00, <tt>false</tt> otherwise.
     */
    public boolean isPositive() {
        return (amount != null && amount.compareTo(BigDecimal.ZERO) > 0);
    }

    /**
     * Determines if the value is filled and less than 0.00
     *
     * @return <tt>true</tt> if this value is filled and less than 0.00, <tt>false</tt> otherwise.
     */
    public boolean isNegative() {
        return (amount != null && amount.compareTo(BigDecimal.ZERO) < 0);
    }

    /**
     * Determines if the value is empty or equal to 0.00
     *
     * @return <tt>true</tt> if this value is empty, or equal to 0.00, <tt>false</tt> otherwise.
     */
    public boolean isZeroOrNull() {
        return (amount == null || amount.compareTo(BigDecimal.ZERO) == 0);
    }

    /**
     * Converts a given decimal fraction into a percent value i.e. 0.34 to 34 %.
     * Effectively this is <code>this * 100</code>
     *
     * @return a percentage representation of the given decimal value.
     */
    @Nonnull
    @CheckReturnValue
    public Amount toPercent() {
        return this.times(ONE_HUNDRED);
    }

    /**
     * Converts a percent value into a decimal fraction i.e. 34 % to 0.34
     * Effectively this is <code>this / 100</code>
     *
     * @return a decimal representation fo the given percent value.
     */
    @Nonnull
    @CheckReturnValue
    public Amount asDecimal() {
        return divideBy(ONE_HUNDRED);
    }

    @Override
    public int compareTo(Amount o) {
        if (o == null) {
            return 1;
        }
        if (o == this) {
            return 0;
        }
        if (amount == o.amount) {
            return 0;
        }
        if (amount == null) {
            return -1;
        }
        return amount.compareTo(o.amount);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Amount amount = (Amount) o;
        if (this.amount == null || amount.amount == null) {
            return (this.amount == null) == (amount.amount == null);
        }

        return this.amount.compareTo(amount.amount) == 0;
    }

    @Override
    public int hashCode() {
        return amount != null ? amount.hashCode() : 0;
    }

    @Override
    public String toString() {
        return toSmartRoundedString(NumberFormat.TWO_DECIMAL_PLACES).toString();
    }

    /**
     * Formats the represented value as percentage. Up to two digits will be shown if non zero.
     * Therefore <tt>12.34</tt> will be rendered as <tt>12.34 %</tt> but <tt>5.00</tt> will be
     * rendered as <tt>5 %</tt>. If the value is empty, "" will be returned.
     * <p>
     * A custom {@link NumberFormat} can be used by directly calling {@link #toSmartRoundedString(NumberFormat)}
     * or to disable smart rounding (to also show .00) {@link #toString(NumberFormat)} can be called using
     * {@link NumberFormat#PERCENT}.
     * </p>
     *
     * @return a string representation of this number using <code>NumberFormat#PERCENT</code>
     *         or "" if the value is empty.
     */
    public String toPercentString() {
        return toSmartRoundedString(NumberFormat.PERCENT).toString();
    }

    /**
     * Formats the represented value by rounding to zero decimal places. The rounding mode is obtained from
     * {@link NumberFormat#NO_DECIMAL_PLACES}.
     *
     * @return a rounded representation of this number using <code>NumberFormat#NO_DECIMAL_PLACES</code>
     *         or "" is the value is empty.
     */
    public String toRoundedString() {
        return toSmartRoundedString(NumberFormat.NO_DECIMAL_PLACES).toString();
    }

    /**
     * Rounds the number according to the given format. In contrast to only round values when displaying them as
     * string, this method returns a modified <tt>Amount</tt> which as potentially lost some precision. Depending on
     * the next computation this might return significantly different values in contrast to first performing all
     * computations and round at the end when rendering the values as string.
     * <p>
     * The number of decimal places and the rounding mode is obtained from <tt>format</tt> ({@link NumberFormat}).
     * </p>
     *
     * @return returns an <tt>Amount</tt> which is rounded using the given <code>NumberFormat</code>
     *         or <tt>NOTHING</tt> if the value is empty.
     */
    @Nonnull
    @CheckReturnValue
    public Amount round(@Nonnull NumberFormat format) {
        if (isEmpty()) {
            return NOTHING;
        }

        return Amount.of(amount.setScale(format.getScale(), format.getRoundingMode()));
    }

    private Value convertToString(NumberFormat format, boolean smartRound) {
        if (isEmpty()) {
            return Value.of(null);
        }
        DecimalFormat df = new DecimalFormat();
        df.setMinimumFractionDigits(smartRound ? 0 : format.getScale());
        df.setMaximumFractionDigits(format.getScale());
        df.setDecimalFormatSymbols(format.getDecimalFormatSymbols());
        df.setGroupingUsed(true);
        return Value.of(df.format(amount)).append(" ", format.getSuffix());
    }

    /**
     * Converts the number into a string according to the given <tt>format</tt>. The returned {@link Value} provides
     * helpful methods to pre- or append texts like units or currency symbols while gracefully handling empty values.
     *
     * @param format the <code>NumberFormat</code> used to obtain the number of decimal places,
     *               the decimal format symbols and rounding mode
     * @return a <tt>Value</tt> containing the string representation according to the given format
     *         or an empty <tt>Value</tt> if <tt>this</tt> is empty.
     * @see Value#append(String, Object)
     * @see Value#prepend(String, Object)
     */
    @Nonnull
    public Value toString(@Nonnull NumberFormat format) {
        return convertToString(format, false);
    }

    /**
     * Converts the number into a string just like {@link #toString(NumberFormat)}. However, if the number has no
     * decimal places, a rounded value (without .00) will be returned.
     *
     * @param format the <code>NumberFormat</code> used to obtain the number of decimal places,
     *               the decimal format symbols and rounding mode
     * @return a <tt>Value</tt> containing the string representation according to the given format
     *         or an empty <tt>Value</tt> if <tt>this</tt> is empty. Omits 0 as decimal places.
     * @see #toString()
     */
    @Nonnull
    public Value toSmartRoundedString(@Nonnull NumberFormat format) {
        return convertToString(format, true);
    }

    private static final String[] METRICS = new String[]{"f", "n", "u", "m", "", "K", "M", "G"};
    private static int NEUTRAL_METRIC = 4;

    /**
     * Creates a "scientific" representation of the amount.
     * <p>
     * This representation will shift the value in the range 0..999 and represent the decimal shift by a well
     * known unit prefix. The following prefixes will be used:
     * <ul>
     * <li>f - femto</li>
     * <li>n - nano</li>
     * <li>u - micro</li>
     * <li>m - milli</li>
     * <li>K - kilo</li>
     * <li>M - mega</li>
     * <li>G - giga</li>
     * </ul>
     * </p>
     * <p>
     * An input of <tt>0.0341 V</tt> will be represented as <tt>34.1 mV</tt> if digits was 4 or 34 mV is digits was 2
     * or less.
     * </p>
     *
     * @param digits the number of decimal digits to display
     * @param unit   the unit to be appended to the generated string
     * @return a scientific string representation of the amount.
     */
    @Nonnull
    public String toScientificString(int digits, String unit) {
        if (isEmpty()) {
            return "";
        }
        int metric = NEUTRAL_METRIC;
        double value = amount.doubleValue();
        if (value != 0d) {
            while (Math.abs(value) >= 990d && metric < METRICS.length - 1) {
                value /= 1000d;
                metric += 1;
            }
            if (metric == NEUTRAL_METRIC) {
                while (Math.abs(value) < 1.01 && metric > 0) {
                    value *= 1000d;
                    metric -= 1;
                    // We loose accuracy, therefore we limit our decimal digits...
                    digits -= 3;
                }
            }
        }
        DecimalFormat df = new DecimalFormat();
        df.setMinimumFractionDigits(Math.max(0, digits));
        df.setMaximumFractionDigits(Math.max(0, digits));
        df.setDecimalFormatSymbols(NLS.getDecimalFormatSymbols());
        df.setGroupingUsed(true);
        if (unit != null) {
            return df.format(value) + " " + METRICS[metric] + unit;
        } else if (metric != NEUTRAL_METRIC) {
            return df.format(value) + " " + METRICS[metric];
        } else {
            return df.format(value);
        }
    }

    /**
     * Returns the number of decimal digits (ignoring decimal places after the decimal separator).
     *
     * @return the number of digits required to represent this number. Returns 0 if the value is empty.
     */
    public long getDigits() {
        if (amount == null) {
            return 0;
        }
        return Math.round(Math.floor(Math.log10(amount.doubleValue()) + 1));
    }
}
