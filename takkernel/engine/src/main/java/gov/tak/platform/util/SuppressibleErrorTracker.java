package gov.tak.platform.util;

import com.atakmap.util.ConfigOptions;
import gov.tak.api.annotation.NonNull;
import gov.tak.api.annotation.Nullable;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiFunction;

/**
 * Utility class to track errors and determine if a given error should be reported.
 * <p/>
 * A default set of error classifiers is included that will handle the following error types:
 * <ul>
 *     <li>{@code FileNotFound} - tracking {@link FileNotFoundException}</li>
 *     <li>{@code Forbidden} - tracking {@link IOException} that has a message containing {@code "403"}</li>
 *     <li>{@code NoRouteToHost} - tracking {@link NoRouteToHostException}</li>
 *     <li>{@code Timeout} - tracking {@link SocketTimeoutException}</li>
 *     <li>{@code UnknownHost} - tracking {@link UnknownHostException}</li>
 * </ul>
 * Additional default classifiers, used by all instances on this class, can be added using {@link #addDefaultErrorClassifiers(ErrorClassifier...)}.
 *
 * @see ErrorType
 * @see ErrorClassifier
 * @see #addDefaultErrorClassifiers(ErrorClassifier...)
 * @since 7.11
 */
public class SuppressibleErrorTracker
{
    private static final boolean suppressDuplicateLogs = ConfigOptions.getOption("logging.suppress-duplicate-logs", 0) != 0;

    /**
     * @return {@code true} if suppression of duplicate IO errors is configured
     */
    public static boolean suppressDuplicateLogs()
    {
        return suppressDuplicateLogs;
    }

    /**
     * Classifies errors that can occur, allowing for suppression of repeated errors of a given type.
     */
    public static final class ErrorType
    {
        private final String name;

        /**
         * @param name A descriptive name that can be used in error messages
         */
        public ErrorType(String name)
        {
            this.name = name;
        }

        public String name()
        {
            return name;
        }
    }

    /**
     * An error type that should not be suppressed.
     */
    public static final ErrorType UnsuppressibleError = new ErrorType("UnsuppressibleError");

    /**
     * Contract for classifying an exception into an instance of {@link ErrorType}.  Error types are compared using
     * identity, not equality.  It is up to the implementation to ensure that a single unique instance of
     * {@link ErrorType} is returned for all exceptions to be classified together.
     * <p/>
     * This function will be passed an exception instance, and an optional (maybe {@code null}) "extra" object.
     * It is up to client code to pass said extra data when calling {@link SuppressibleErrorTracker#getTypeForException(Exception, Object)}.
     * If no extra data is needed to classify an exception, then clients should call {@link SuppressibleErrorTracker#getTypeForException(Exception)}.
     * <p/>
     * Classifiers that have no opinion on a given exception should return {@code null}.  If they want to force
     * an exception to be unsuppressible, then they should return {@link #UnsuppressibleError}.
     */
    public interface ErrorClassifier extends BiFunction<Exception, Object, ErrorType>
    {
    }

    /**
     * An error classifier that uses a simple {@code instanceof} check on the given exception.
     * Extra data passed from {@link #getTypeForException(Exception, Object)} is ignored.
     * <p/>
     * Note that two instances of this class using the same exception class will produce <em>distinct</em> error
     * types, and will be suppressed separately from each other.
     */
    public static final class InstanceofClassifier implements ErrorClassifier
    {
        private final Class<Exception> exceptionClass;
        private final ErrorType errorType;

        /**
         * @param exceptionClass Class of exceptions to match
         * @see Class#isInstance(Object)
         */
        public InstanceofClassifier(Class<Exception> exceptionClass)
        {
            this.exceptionClass = exceptionClass;
            errorType = new ErrorType(exceptionClass.getSimpleName());
        }

        @Override
        public ErrorType apply(Exception e, Object extra)
        {
            return exceptionClass.isInstance(e) ? errorType : null;
        }
    }

    // Classifiers used by ALL instances
    private static final List<ErrorClassifier> DEFAULT_CLASSIFIERS = new CopyOnWriteArrayList<>();

    static
    {
        DEFAULT_CLASSIFIERS.add(new DefaultClassifier());
    }

    /**
     * Add error classifiers to be used by all instances.
     */
    public static void addDefaultErrorClassifiers(ErrorClassifier... errorClassifiers)
    {
        DEFAULT_CLASSIFIERS.addAll(Arrays.asList(errorClassifiers));
    }

    private final boolean includeDefaultClassifiers;
    private final List<ErrorClassifier> errorClassifiers = new ArrayList<>();

    // Tracks whether an error has been logged for a given ErrorType
    private final Set<ErrorType> errorReported = Collections.newSetFromMap(new IdentityHashMap<>());

    /**
     * Constructor to include the default classifiers, and any additional classifiers specified.
     *
     * @see #addDefaultErrorClassifiers(ErrorClassifier...)
     */
    public SuppressibleErrorTracker(ErrorClassifier... errorClassifiers)
    {
        this(true, errorClassifiers);
    }

    /**
     * Constructor to include or exclude the default classifiers, and any additional classifiers specified.
     *
     * @param includeDefaultClassifiers  Pass {@code true} to include the default classifiers, {@code false} to exclude them
     * @param additionalErrorClassifiers Additional error classifiers to use, if any
     * @see #addDefaultErrorClassifiers(ErrorClassifier...)
     */
    public SuppressibleErrorTracker(boolean includeDefaultClassifiers, ErrorClassifier... additionalErrorClassifiers)
    {
        this.includeDefaultClassifiers = includeDefaultClassifiers;
        errorClassifiers.addAll(Arrays.asList(additionalErrorClassifiers));
    }

    /**
     * @return The appropriate {@link ErrorType} for the given exception, or {@link #UnsuppressibleError} if not a suppressible type.
     * <p/>
     * Equivalent to calling {@link #getTypeForException(Exception, Object) getTypeForException(e, null)}.
     */
    @NonNull
    public ErrorType getTypeForException(@NonNull Exception e)
    {
        return getTypeForException(e, null);
    }

    /**
     * @param e     Exception that occurred
     * @param extra Extra info to pass to registered {@link ErrorClassifier classifiers}
     * @return The appropriate error type for the given exception, or {@link #UnsuppressibleError} if not a suppressible type
     */
    @NonNull
    public ErrorType getTypeForException(@NonNull Exception e, @Nullable Object extra)
    {
        Objects.requireNonNull(e, "'e' must not be null");

        ErrorType errorType;

        if (includeDefaultClassifiers)
        {
            errorType = classify(e, extra, DEFAULT_CLASSIFIERS);
            if (errorType != null) return errorType;
        }

        errorType = classify(e, extra, errorClassifiers);
        if (errorType != null) return errorType;

        return UnsuppressibleError;
    }

    /**
     * Classify a given exception using the given list of classifiers.
     */
    private ErrorType classify(Exception e, Object extra, List<ErrorClassifier> classifiers)
    {
        for (ErrorClassifier classifier : classifiers)
        {
            final ErrorType errorType = classifier.apply(e, extra);
            if (errorType != null) return errorType;
        }
        return null;
    }

    /**
     * Record an error of the given error type.  If error suppression is configured, according to
     * {@link #suppressDuplicateLogs()}, and this is not the first time the error has been recorded, then this
     * method will return {@code true}.  Otherwise, it will return {@code false}, indicating that the error should
     * be reported.  This method will always return {@code false} when given {@link #UnsuppressibleError}
     * as an argument.
     *
     * @param errorType Error type to record
     * @return {@code true} if reporting of the recorded error should be suppressed
     * @see #suppressDuplicateLogs()
     */
    public boolean record(@NonNull ErrorType errorType)
    {
        Objects.requireNonNull(errorType, "'errorType' must not be null");

        if (errorType == UnsuppressibleError)
        {
            return false;
        } else
        {
            return suppressDuplicateLogs && !errorReported.add(errorType);
        }
    }

    private static final class DefaultClassifier implements ErrorClassifier
    {
        private static final ErrorType FileNotFound = new ErrorType("FileNotFound");
        private static final ErrorType Forbidden = new ErrorType("Forbidden");
        private static final ErrorType NoRouteToHost = new ErrorType("NoRouteToHost");
        private static final ErrorType Timeout = new ErrorType("Timeout");
        private static final ErrorType UnknownHost = new ErrorType("UnknownHost");

        @Override
        public ErrorType apply(Exception e, Object extra)
        {
            if (e instanceof SocketTimeoutException)
            {
                return Timeout;
            } else if (e instanceof FileNotFoundException)
            {
                return FileNotFound;
            } else if (e instanceof NoRouteToHostException)
            {
                return NoRouteToHost;
            } else if (e instanceof UnknownHostException)
            {
                return UnknownHost;
            } else if (e instanceof IOException && e.getMessage().contains("403"))
            {
                return Forbidden;
            }

            return null;
        }
    }
}
