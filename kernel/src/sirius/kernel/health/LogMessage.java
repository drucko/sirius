/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.health;

import org.apache.log4j.Level;

/**
 * Contains a log message passed from {@link Log} to {@link LogTap}.
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/11
 */
public class LogMessage {
    private String message;
    private Level logLevel;
    private Log receiver;
    boolean receiverWouldLog;

    protected LogMessage(String message, Level logLevel, Log receiver, boolean receiverWouldLog) {
        this.message = message;
        this.logLevel = logLevel;
        this.receiver = receiver;
        this.receiverWouldLog = receiverWouldLog;
    }

    /**
     * Contains the logged message.
     *
     * @return the message sent to the logger
     */
    public String getMessage() {
        return message;
    }

    /**
     * Contains the log level used for this message.
     *
     * @return the log level of this message
     */
    public Level getLogLevel() {
        return logLevel;
    }

    /**
     * Returns the logger used to handle this message.
     *
     * @return the logger used to handle this message
     */
    public Log getReceiver() {
        return receiver;
    }

    /**
     * Returns whether the receiver has logged this message.
     *
     * @return wether the receiver has logged this message
     */
    public boolean isReceiverWouldLog() {
        return receiverWouldLog;
    }
}