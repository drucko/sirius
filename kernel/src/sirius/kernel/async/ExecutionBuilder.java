/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.async;

import sirius.kernel.health.Exceptions;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Builder pattern for forking or starting sub tasks.
 * <p>
 * Used by {@link Async#executor(String)} to construct an execution for a given subtask. Can be used to specify whether
 * the current {@link CallContext} is forked, or if a new one is started. Also one can specify whether and how a given
 * task might be dropped on system overload conditions.
 * </p>
 * <p>
 * Most of the time this builder will be used to either call {@link #fork(Runnable)} or {@link #start(Runnable)}
 * to either fork the current <tt>CallContext</tt> or to start a sub task with a new one. Also a drop handler can be
 * supplied using {@link #dropOnOverload(Runnable)} to gracefully handle system overload conditions.
 * </p>
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/08
 */
@ParametersAreNonnullByDefault
public class ExecutionBuilder<R> {

    /**
     * Internal class which takes care of passing along the CallContext and for storing the configuration made by the
     * ExecutionBuilder.
     */
    static class TaskWrapper implements Runnable {
        String category;
        Runnable runnable;
        boolean fork;
        Runnable dropHandler;
        CallContext callContext;
        Future promise = Async.future();

        /**
         * Prepares the execution of this task while checking all preconditions.
         */
        void prepare() {
            if (fork) {
                callContext = CallContext.getCurrent();
            }
            if (runnable == null) {
                throw new IllegalArgumentException("Please provide a runnable for me to execute!");
            }
        }

        @Override
        public void run() {
            try {
                CallContext ctx = CallContext.getCurrent();
                try {
                    if (callContext == null) {
                        CallContext.initialize();
                    } else {
                        CallContext.setCurrent(callContext);
                    }
                    runnable.run();
                    promise.success(null);
                } finally {
                    CallContext.setCurrent(ctx);
                }
            } catch (Throwable t) {
                Exceptions.handle(Async.LOG, t);
                promise.fail(t);
            }
        }

        @Override
        public String toString() {
            return category;
        }
    }

    private TaskWrapper wrapper = new TaskWrapper();

    /**
     * Generates a new ExecutionBuilder for the given category.
     *
     * @param category the category which is used to determine which executor to use
     */
    ExecutionBuilder(String category) {
        wrapper.category = category;
    }

    /**
     * Specifies to fork the current CallContext while executing the given task.
     *
     * @param task the task to execute.
     * @return this for fluent builder calls.
     */
    public ExecutionBuilder<R> fork(Runnable task) {
        wrapper.runnable = task;
        wrapper.fork = true;
        return this;
    }

    /**
     * Specifies to create a new CallContext while executing the given task.
     *
     * @param task the task to execute.
     * @return this for fluent builder calls.
     */
    public ExecutionBuilder<R> start(Runnable task) {
        wrapper.runnable = task;
        wrapper.fork = false;
        return this;
    }

    /**
     * Specifies that the given task can be dropped (ignored) in system overload conditions, if at least the given
     * handler is called.
     *
     * @param dropHandler the handler which is informed if the task is dropped due to system overload conditions.
     * @return this for fluent builder calls.
     */
    public ExecutionBuilder<R> dropOnOverload(Runnable dropHandler) {
        wrapper.dropHandler = dropHandler;
        return this;
    }

    /**
     * Creates and submits a task based on the made specifications
     *
     * @return a Future representing the execution created by this builder.
     */
    public Future execute() {
        Async.execute(wrapper);
        return wrapper.promise;
    }

}
