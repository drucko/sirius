/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons;

import sirius.kernel.nls.NLS;

import java.io.IOException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Set;
import java.util.TreeSet;

/**
 * A flexible parser which can parse dates like DD.MM.YYYY or YYYY/DD/MM along with some computations.
 * <p>
 * A valid expression is defined by the following grammar:</p>
 * </p>
 * <ul>
 * <li><code><b>ROOT</b> ::= (MODIFIER ",")* (":")? ("now" | DATE)? (("+" | "-") NUMBER (UNIT)?)</code></li>
 * <li><code><b>UNIT</b> ::= ("day" | "days" | "week" | "weeks" | "month" | "months" | "year" | "years")</code></li>
 * <li><code><b>NUMBER</b> ::=(0-9)+</code></li>
 * <li><code><b>DATE</b> ::= GERMAN_DATE | ENGLISH_DATE | YM_EXPRESSION</code></li>
 * <li><code><b>GERMAN_DATE</b> ::= NUMBER "." NUMBER ("." NUMBER)? (NUMBER (":" NUMBER (":" NUMBER)?)?)?</code></li>
 * <li><code><b>ENGLISH_DATE</b> ::=  NUMBER "/" NUMBER ("/" NUMBER)? (NUMBER (":" NUMBER (":" NUMBER)?)?)? ("am" | "pm")?)?</code></li>
 * <li><code><b>YM_EXPRESSION</b> ::= NUMBER</code></li>
 * <li><code><b>MODIFIER</b> ::= ("start" | "end") ("of")? ("day" | "week" | "month" | "year")</code></li>
 * </ul>
 * <p/>
 * <b>Examples</b>
 * <ul>
 * <li><code>now</code> - actual date</li>
 * <li><code>1.1</code> - first of january of current year</li>
 * <li><code>+1</code> or <code>now + 1 day</code> - tomorrow</li>
 * <li><code>start of week: now - 1 year</code> - start of the week of day one year ago</li>
 * </ul>
 *
 * @author Andreas Haufler (aha@scireum.de)
 */
public class AdvancedDateParser {

    private String lang;

    /**
     * Creates a new parser for the given language to use.
     *
     * @param lang contains the two letter language code to obtain the translations for the available modifiers etc.
     */
    public AdvancedDateParser(String lang) {
        this.lang = lang;
    }

    private static final String NEGATIVE_DELTA = "-";
    private static final String POSITIVE_DELTA = "+";
    private static final String MODIFIER_END = ":";
    private static final String MODIFIER_SEPARATOR = ",";
    private static final String PM = "pm";
    private static final String AM = "am";
    private static final String TIME_SEPARATOR = ":";
    private static final String ENGLISH_DATE_SEPARATOR = "/";
    private static final String GERMAN_DATE_SEPARATOR = ".";

    /*
     * Used to tokenize the input supplied by the user
     */
    class Tokenizer {

        public static final int END_OF_INPUT = 1;
        public static final int NUMBER = 2;
        public static final int IDENTIFIER = 3;
        public static final int SPECIAL = 4;
        private final String input;
        private String nextToken;
        private int type;
        private int tokenStart = 0;
        private int position = 0;

        Tokenizer(String inputString) {
            this.input = inputString;
        }

        /*
         * Reads the next token in the input
         */
        void nextToken() {
            nextToken = "";
            while (endOfInput()) {
                if (isDigit()) {
                    readNumber();
                    return;
                } else if (isLetter()) {
                    readIdentifier();
                    return;
                } else if (isWhitespace()) {
                    // Ignore whitespaces
                } else {
                    readSpecialChars();
                    return;
                }
                position++;
            }
            type = END_OF_INPUT;
            return;
        }

        private boolean isWhitespace() {
            return Character.isWhitespace(input.charAt(position));
        }

        private boolean isLetter() {
            return Character.isLetter(input.charAt(position));
        }

        private boolean endOfInput() {
            return position < input.length();
        }

        private boolean isDigit() {
            return Character.isDigit(input.charAt(position));
        }

        private void readSpecialChars() {
            tokenStart = position;
            type = SPECIAL;
            nextToken += input.charAt(position);
            position++;
        }

        private void readIdentifier() {
            tokenStart = position;
            type = IDENTIFIER;
            while ((endOfInput()) && (isLetter())) {
                nextToken += input.charAt(position);
                position++;
            }
        }

        private void readNumber() {
            tokenStart = position;
            type = NUMBER;
            while ((endOfInput()) && (isDigit())) {
                nextToken += input.charAt(position);
                position++;
            }
        }

        @Override
        public String toString() {
            return NLS.fmtr("AdvancedDateParser.tokenizerMessage")
                      .set("nextToken", nextToken)
                      .set("tokenStart", tokenStart)
                      .set("tokenEnd", position)
                      .format();
        }

        /*
         * Returns the current token
         */
        String getToken() {
            return nextToken;
        }

        /*
         * Returns the type of the current token
         */
        int getType() {
            return type;
        }

        /*
         * Returns the start pos of the current toke
         */
        int getTokenStart() {
            return tokenStart;
        }

        /*
         * Returns the position of the next character checked by the tokenizer.
         */
        int getPosition() {
            return position;
        }
    }

    private Tokenizer tokenizer;
    private boolean startOfDay = false;
    private boolean startOfWeek = false;
    private boolean startOfMonth = false;
    private boolean startOfYear = false;
    private boolean endOfDay = false;
    private boolean endOfWeek = false;
    private boolean endOfMonth = false;
    private boolean endOfYear = false;

    /**
     * Parses the given input and returns a <tt>DateSelection</tt> as result.
     * <p>
     * Note that an <tt>AdvancedDateParser</tt> is stateful an is therefore neither reusable nor thread-safe.
     * </p>
     *
     * @param input the text to parse
     * @return a <tt>DateSelection</tt> containing the effective date along with the parsed expression
     * @throws ParseException if the input cannot be parsed as it does not conform to the given grammar
     */
    public DateSelection parse(String input) throws ParseException {
        startOfDay = false;
        startOfWeek = false;
        startOfMonth = false;
        startOfYear = false;
        endOfDay = false;
        endOfWeek = false;
        endOfMonth = false;
        endOfYear = false;
        if (Strings.isEmpty(input)) {
            return null;
        }
        input = eliminateTextInBrackets(input);
        try {
            tokenizer = new Tokenizer(input.toLowerCase());
            do {
                tokenizer.nextToken();
                // ignore "," between modifiers
                if ((tokenizer.getType() == Tokenizer.SPECIAL) && (in(MODIFIER_SEPARATOR))) {
                    tokenizer.nextToken();
                }
            } while (parseModifier());
            // ignore ":" after modifiers
            if ((tokenizer.getType() == Tokenizer.SPECIAL) && (in(MODIFIER_END))) {
                tokenizer.nextToken();
            }
            Calendar result = parseFixPoint();
            while (tokenizer.getType() != Tokenizer.END_OF_INPUT) {
                parseDelta(result, tokenizer);
                tokenizer.nextToken();
            }
            applyModifiers(result);
            return new DateSelection(result, input);
        } catch (IOException e) {
            throw new ParseException(e.getMessage(), 0);
        }
    }

    /*
     * The text representation of a DateSelection contains the effective value in brackets. If we re-parse the string,
     * we simply cut this block.
     */
    private String eliminateTextInBrackets(String input) {
        int first = input.indexOf("[");
        int last = input.lastIndexOf("]");
        if (first < 0) {
            return input.trim();
        }
        String result = input.substring(0, first);
        if ((last > -1) && (last < input.length() - 1)) {
            result += input.substring(last + 1, input.length());
        }
        return result.trim();
    }

    /*
     * Applies the parsed modifiers to the previously calculated result.
     */
    private void applyModifiers(Calendar result) {
        // Force conversion
        result.getTime();
        if (startOfYear) {
            result.set(Calendar.DAY_OF_MONTH, 1);
            result.set(Calendar.MONTH, Calendar.JANUARY);
            // Force conversion
            result.getTime();
        }
        if (endOfYear) {
            result.set(Calendar.DAY_OF_MONTH, 31);
            result.set(Calendar.MONTH, Calendar.DECEMBER);
            // Force conversion
            result.getTime();
        }
        if (startOfMonth) {
            result.set(Calendar.DAY_OF_MONTH, 1);
            // Force conversion
            result.getTime();
        }
        if (endOfMonth) {
            result.set(Calendar.DAY_OF_MONTH, result.getActualMaximum(Calendar.DAY_OF_MONTH));
            // Force conversion
            result.getTime();
        }
        if (startOfWeek) {
            result.set(Calendar.DAY_OF_WEEK, result.getFirstDayOfWeek());
            // Force conversion
            result.getTime();
        }
        if (endOfWeek) {
            result.set(Calendar.DAY_OF_WEEK, (result.getFirstDayOfWeek() + 6) % 7);
            // Force conversion
            result.getTime();
        }
        if (startOfDay) {
            result.set(Calendar.MILLISECOND, 0);
            result.set(Calendar.SECOND, 0);
            result.set(Calendar.MINUTE, 0);
            result.set(Calendar.HOUR_OF_DAY, 0);
            // Force conversion
            result.getTime();
        }
        if (endOfDay) {
            result.set(Calendar.MILLISECOND, 999);
            result.set(Calendar.SECOND, 59);
            result.set(Calendar.MINUTE, 59);
            result.set(Calendar.HOUR_OF_DAY, 23);
            // Force conversion
            result.getTime();
        }
    }

    private String[] getI18n(String key) {
        return getI18nText(key).split(MODIFIER_SEPARATOR);
    }

    private String getI18nText(String key) {
        return NLS.get(key, lang);
    }

    private boolean parseModifier() throws ParseException {
        if (!(tokenizer.getType() == Tokenizer.IDENTIFIER)) {
            return false;
        }
        if (in(start())) {
            parseStartModifier();
            return true;
        }
        if (in(end())) {
            parseEndModifier();
            return true;
        }
        return false;
    }

    private String[] end() {
        return join(getI18n("AdvancedDateParser.end"), new String[]{"end"});
    }

    private String[] calendarWeek() {
        return join(getI18n("AdvancedDateParser.calendarWeek"), new String[]{"week"});
    }

    private void parseStartModifier() throws ParseException {
        tokenizer.nextToken();
        expectKeyword(join(of(), day(), week(), month(), year()));
        if (in(of())) {
            tokenizer.nextToken();
            expectKeyword(join(day(), week(), month(), year()));
        }
        if (in(day())) {
            startOfDay = true;
        }
        if (in(week())) {
            startOfWeek = true;
        }
        if (in(month())) {
            startOfMonth = true;
        }
        if (in(year())) {
            startOfYear = true;
        }
    }

    private void parseEndModifier() throws ParseException {
        tokenizer.nextToken();
        expectKeyword(join(of(), day(), week(), month(), year()));
        if (in(of())) {
            tokenizer.nextToken();
            expectKeyword(join(day(), week(), month(), year()));
        }
        if (in(day())) {
            endOfDay = true;
        }
        if (in(week())) {
            endOfWeek = true;
        }
        if (in(month())) {
            endOfMonth = true;
        }
        if (in(year())) {
            endOfYear = true;
        }
    }

    private String[] of() {
        return join(getI18n("AdvancedDateParser.of"), new String[]{"of"});
    }

    private String[] day() {
        return join(getI18n("AdvancedDateParser.day"), new String[]{"day"});
    }

    private String[] week() {
        return join(getI18n("AdvancedDateParser.week"), new String[]{"week"});
    }

    private String[] month() {
        return join(getI18n("AdvancedDateParser.month"), new String[]{"month"});
    }

    private String[] year() {
        return join(getI18n("AdvancedDateParser.year"), new String[]{"year"});
    }

    private String[] start() {
        return join(getI18n("AdvancedDateParser.start"), new String[]{"start"});
    }

    private void parseDelta(Calendar fixPoint, Tokenizer tokenizer) throws ParseException, IOException {
        expectKeyword(POSITIVE_DELTA, NEGATIVE_DELTA);
        boolean add = true;
        if (POSITIVE_DELTA.equals(tokenizer.getToken())) {
            add = true;
        } else if (NEGATIVE_DELTA.equals(tokenizer.getToken())) {
            add = false;
        }
        tokenizer.nextToken();
        expectNumber();
        int amount = Integer.parseInt(tokenizer.getToken());
        if (!add) {
            amount *= -1;
        }
        tokenizer.nextToken();
        if (tokenizer.getType() == Tokenizer.END_OF_INPUT) {
            fixPoint.add(Calendar.DAY_OF_MONTH, amount);
            return;
        }
        expectKeyword(join(seconds(), minutes(), hours(), days(), weeks(), months(), years()));
        if (in(seconds())) {
            fixPoint.add(Calendar.SECOND, amount);
            return;
        }
        if (in(minutes())) {
            fixPoint.add(Calendar.MINUTE, amount);
            return;
        }
        if (in(hours())) {
            fixPoint.add(Calendar.HOUR, amount);
            return;
        }
        if (in(days())) {
            fixPoint.add(Calendar.DAY_OF_MONTH, amount);
            return;
        }
        if (in(weeks())) {
            fixPoint.add(Calendar.DAY_OF_MONTH, amount * 7);
            return;
        }
        if (in(months())) {
            fixPoint.add(Calendar.MONTH, amount);
            return;
        }
        if (in(years())) {
            fixPoint.add(Calendar.YEAR, amount);
            return;
        }
    }

    private String[] years() {
        return join(getI18n("AdvancedDateParser.years"), new String[]{"year", "years"});
    }

    private String[] months() {
        return join(getI18n("AdvancedDateParser.months"), new String[]{"month", "months"});
    }

    private String[] weeks() {
        return join(getI18n("AdvancedDateParser.weeks"), new String[]{"week", "weeks"});
    }

    private String[] days() {
        return join(getI18n("AdvancedDateParser.days"), new String[]{"day", "days"});
    }

    private String[] hours() {
        return join(getI18n("AdvancedDateParser.hours"), new String[]{"hour", "hours"});
    }

    private String[] minutes() {
        return join(getI18n("AdvancedDateParser.minutes"), new String[]{"minute", "minutes"});
    }

    private String[] seconds() {
        return join(getI18n("AdvancedDateParser.seconds"), new String[]{"second", "seconds"});
    }

    private String[] join(String[]... arrays) {
        Set<String> values = new TreeSet<String>();
        for (String[] array : arrays) {
            for (String value : array) {
                values.add(value);
            }
        }
        return values.toArray(new String[values.size()]);
    }

    private Calendar parseFixPoint() throws IOException, ParseException {
        if (tokenizer.getType() == Tokenizer.NUMBER) {
            return parseDate(tokenizer);
        }
        if (tokenizer.getType() == Tokenizer.SPECIAL) {
            return now();
        }
        if (tokenizer.getType() == Tokenizer.END_OF_INPUT) {
            return now();
        }
        if (in(calendarWeek())) {
            tokenizer.nextToken();
            expectNumber();
            Calendar result = now();
            result.set(Calendar.WEEK_OF_YEAR, Integer.parseInt(tokenizer.getToken()));
            result.getTime();
            tokenizer.nextToken();
            return result;
        }
        expectKeyword(nowToken());
        tokenizer.nextToken();
        return now();
    }

    private String[] nowToken() {
        return join(getI18n("AdvancedDateParser.now"), new String[]{"now"});
    }

    private Calendar now() {
        return Calendar.getInstance();
    }

    private void expectNumber() throws ParseException {
        if (!(tokenizer.getType() == Tokenizer.NUMBER)) {
            throw new ParseException(NLS.fmtr("AdvancedDateParser.errInvalidToken")
                                        .set("token", tokenizer.toString())
                                        .format(), tokenizer.getTokenStart());
        }
    }

    private boolean in(String... keywords) {
        for (String keyword : keywords) {
            if (keyword.equals(tokenizer.getToken())) {
                return true;
            }
        }
        return false;
    }

    private void expectKeyword(String... keywords) throws ParseException {
        if (!in(keywords)) {
            StringBuilder allKeyWords = new StringBuilder();
            for (String keyword : keywords) {
                allKeyWords.append(", ");
                allKeyWords.append("'");
                allKeyWords.append(keyword);
                allKeyWords.append("'");
            }
            throw new ParseException(NLS.fmtr("AdvancedDateParser.errUnexpectedKeyword")
                                        .set("token", tokenizer.toString())
                                        .set("keywords", allKeyWords.substring(2))
                                        .format(), tokenizer.getTokenStart());
        }
    }

    private Calendar parseDate(Tokenizer tokenizer) throws ParseException {
        expectNumber();
        int firstNumber = Integer.parseInt(tokenizer.getToken());
        tokenizer.nextToken();
        if (!GERMAN_DATE_SEPARATOR.equals(tokenizer.getToken()) && !ENGLISH_DATE_SEPARATOR.equals(tokenizer.getToken())) {
            return parseYMExpression(firstNumber);
        }
        expectKeyword(GERMAN_DATE_SEPARATOR, ENGLISH_DATE_SEPARATOR);
        if (GERMAN_DATE_SEPARATOR.equals(tokenizer.getToken())) {
            return parseGermanDate(firstNumber);
        } else {
            return parseEnglishDate(firstNumber);
        }
    }

    /*
     * Parses YM expressions: 200903 will be March 2009, 0903 will be converted
     * into the same. 9910 is October 1999.
     */
    private Calendar parseYMExpression(int number) {
        // Convert short format like 0801 or 9904 into the equivalent long
        // format.
        if (number < 6000) {
            // everything below 6000 is considered to be in the 21th century:
            // therefore 6001 is January 1960, 5901 is January 2059.
            number += 200000;
        }
        if (number < 9999) {
            // handle short form of 19th century
            number += 190000;
        }
        int year = number / 100;
        int month = number % 100;
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.MILLISECOND, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.HOUR, 0);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.MONTH, month - 1);
        cal.set(Calendar.YEAR, year);
        return cal;
    }

    private Calendar parseEnglishDate(int month) throws ParseException {
        tokenizer.nextToken();
        expectNumber();
        int day = Integer.parseInt(tokenizer.getToken());
        tokenizer.nextToken();
        int year = now().get(Calendar.YEAR);
        if (in(ENGLISH_DATE_SEPARATOR)) {
            tokenizer.nextToken();
            if (tokenizer.getType() == Tokenizer.NUMBER) {
                year = Integer.parseInt(tokenizer.getToken());
                year = fixYear(year);
                tokenizer.nextToken();
            }
        }
        if (tokenizer.getType() == Tokenizer.NUMBER) {
            return parseTime(buildCalendar(day, month, year));
        }
        return buildCalendar(day, month, year);
    }

    private Calendar parseTime(Calendar result) throws ParseException {
        int hour = Integer.parseInt(tokenizer.getToken());
        tokenizer.nextToken();
        int minute = 0;
        if (in(TIME_SEPARATOR)) {
            tokenizer.nextToken();
            expectNumber();
            minute = Integer.parseInt(tokenizer.getToken());
            tokenizer.nextToken();
        }
        int second = 0;
        if (in(TIME_SEPARATOR)) {
            tokenizer.nextToken();
            expectNumber();
            second = Integer.parseInt(tokenizer.getToken());
            tokenizer.nextToken();
        }
        if (in(AM)) {
            result.set(Calendar.HOUR, hour);
            result.set(Calendar.AM_PM, Calendar.AM);
            tokenizer.nextToken();
        } else if (in(PM)) {
            result.set(Calendar.HOUR, hour);
            result.set(Calendar.AM_PM, Calendar.PM);
            result.set(Calendar.HOUR, hour);
            tokenizer.nextToken();
        } else {
            result.set(Calendar.HOUR_OF_DAY, hour);
        }
        result.set(Calendar.MINUTE, minute);
        result.set(Calendar.SECOND, second);
        return result;
    }

    private Calendar buildCalendar(int day, int month, int year) {
        Calendar result = now();
        result.set(Calendar.MILLISECOND, 0);
        result.set(Calendar.SECOND, 0);
        result.set(Calendar.MINUTE, 0);
        result.set(Calendar.HOUR_OF_DAY, 0);
        result.set(Calendar.YEAR, year);
        result.set(Calendar.MONTH, month - 1);
        result.set(Calendar.DAY_OF_MONTH, day);
        result.getTime();
        return result;
    }

    private Calendar parseGermanDate(int day) throws ParseException {
        tokenizer.nextToken();
        expectNumber();
        int month = Integer.parseInt(tokenizer.getToken());
        tokenizer.nextToken();
        int year = now().get(Calendar.YEAR);
        if (in(GERMAN_DATE_SEPARATOR)) {
            tokenizer.nextToken();
            if (tokenizer.getType() == Tokenizer.NUMBER) {
                year = Integer.parseInt(tokenizer.getToken());
                year = fixYear(year);
                tokenizer.nextToken();
            }
        }
        if (tokenizer.getType() == Tokenizer.NUMBER) {
            return parseTime(buildCalendar(day, month, year));
        }
        return buildCalendar(day, month, year);
    }

    private int fixYear(int year) {
        if (year < 50) {
            year += 2000;
        } else if (year < 100) {
            year += 1900;
        }
        return year;
    }

    /**
     * Combines the parsed text along with the effective date (as <tt>Calendar</tt>).
     * <p>
     * The string representation of this contains the effective date in angular brackets. As there are ignored by
     * the parser, the resulting string can be re-parsed to refresh modifiers and computations.
     * </p>
     *
     * @author Andreas Haufler (aha@scireum.de)
     * @since 2013/08
     */
    public static class DateSelection {

        /**
         * Creates a new <tt>DateSelection</tt> for the given calendar and input string.
         *
         * @param calendar   the effective date to be used
         * @param dateString the input string which yielded the given calendar
         */
        public DateSelection(Calendar calendar, String dateString) {
            super();
            this.calendar = calendar;
            this.dateString = dateString;
        }

        private Calendar calendar;
        private String dateString;

        /**
         * Returns the effective date as <tt>Calendar</tt>
         *
         * @return the effective date. This might be <tt>null</tt> if parsing the expression failed.
         */
        public Calendar getCalendar() {
            return calendar;
        }

        /**
         * Returns the text input which was used to compute the effective date.
         *
         * @return the string from which the <tt>calendar</tt> was parsed
         */
        public String getDateString() {
            return dateString;
        }

        @Override
        public String toString() {
            return asString(false);
        }

        /**
         * Returns a string representation of this <tt>DateSelection</tt>
         *
         * @param dateTime determines whether to include the effective date in angular brackets
         * @return the input string used to create this <tt>DateSelection</tt> and if <tt>dateTime</tt> is true,
         *         appended with the effective date surrounded by angular brackets
         */
        public String asString(boolean dateTime) {
            if (dateString == null) {
                return getDateString(dateTime);
            }
            return dateString + " [" + getDateString(dateTime) + "]";
        }

        private String getDateString(boolean dateTime) {
            if (calendar == null) {
                return "";
            }
            return NLS.toUserString(calendar, dateTime);
        }

        /**
         * Returns the effective date as string
         *
         * @return the effective date formatted as string without any time information
         */
        public String getDate() {
            return getDateString(false);
        }

        /**
         * Returns the effective date as string
         *
         * @return the effective date formatted as string with time information
         */
        public String getDateTime() {
            return getDateString(true);
        }

    }
}
