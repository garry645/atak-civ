package gov.tak.api.util;

import java.util.concurrent.Executor;

public final class Async<Output, Input> {
    final Function<Output, Input> _fn;
    final Executor _fnContext;
    Async<?, Output> _then;
    Error _error;
    Executor _errorContext;

    Async(Function<Output, Input> fn) {
        this(fn, null, null, null);
    }

    Async(Function<Output, Input> fn, Error error) {
        this(fn, null, error, null);
    }

    Async(Function<Output, Input> fn, Executor fnContext) {
        this(fn, fnContext, null, null);
    }

    public Async(Function<Output, Input> fn, Executor fnContext, Error error, Executor errorContext) {
        _fn = fn;
        _fnContext = fnContext;
        _error = error;
        _errorContext = errorContext;
    }

    public final void start(final Input arg) {
        if(_fnContext != null)
            _fnContext.execute(new Runnable() {
                @Override
                public void run() {
                    startImpl(arg);
                }
            });
        else
            startImpl(arg);
    }

    private void startImpl(final Input arg) {
        final Output result;
        try {
            result = _fn.then(arg);
        } catch(final Throwable t) {
            if(_error != null) {
                if (_errorContext != null) {
                    _errorContext.execute(new Runnable() {
                        @Override
                        public void run() {
                            _error.error(t);
                        }
                    });
                } else {
                    _error.error(t);
                }
            }
            return;
        }

        if(_then != null)
            _then.start(result);
    }

    public <ThenOutput> Async<ThenOutput, Output> then(Function<ThenOutput, Output> then) {
        return then(then, null, null, null);
    }

    /**
     * Configures the error handler for this {@code Async}'s <I>function</I>.
     *
     * <P>The handler is invoked in <code>function</code>'s execution context.
     *
     * @param error The error handler
     * @return {@code this}
     */
    public Async<Output, Input> error(Error error) {
        return error(error, null);
    }

    /**
     * <P>The <I>function</I> and error handler for the new {@code Async} will adopt the execution
     * context of this {@code Async}'s <I>function</I>.
     *
     * @param then  The <I>function</I> to be executed in a new {@code Async} context on successful
     *              execution of this {@code Async}'s <I>function</I>.
     * @param error The error handler for the
     * @return
     */
    public <ThenOutput> Async<ThenOutput, Output> then(Function<ThenOutput, Output> then, Error error) {
        return then(then, null, error, null);
    }

    public <ThenOutput> Async<ThenOutput, Output> then(Function<ThenOutput, Output> then, Executor thenContext) {
        final Async<ThenOutput, Output> async = new Async(then, thenContext);
        _then = async;

        return async;
    }

    /**
     * Configures the error handler for this {@code Async}'s <I>function</I>.
     *
     * @param error         The error handler
     * @param errorContext  The execution context for the error handler
     * @return {@code this}
     */
    public Async<Output, Input> error(Error error, Executor errorContext) {
        _error = error;
        _errorContext = errorContext;

        return this;
    }

    public <ThenOutput> Async<ThenOutput, Output> then(Function<ThenOutput, Output> then, Executor thenContext, Error error, Executor errorContext) {
        final Async<ThenOutput, Output> async = new Async(then, thenContext, error, errorContext);
        _then = async;
        return async;
    }

    public interface Function<Tf, Vf> {
        Tf then(Vf arg) throws Throwable;
    }

    public interface Error {
        void error(Throwable t);
    }
}
