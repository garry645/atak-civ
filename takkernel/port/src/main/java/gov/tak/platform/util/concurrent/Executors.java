package gov.tak.platform.util.concurrent;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class Executors {
    final static long DEFAULT_THREAD_POOL_TIMEOUT_MILLIS = 1000L;

    private Executors() {}

    /**
     * Creates a new <I>blocking</I> fixed thread pool. Attempts to submit additional tasks will
     * block when the queue is at its maximum size. Threads will automatically exit if idle for
     * more than the timeout period associated with the pool.
     *
     * <P>The following defaults are used:
     * <UL>
     *     <LI>event queue size is {@code threadCount*2}</LI>
     *     <LI>idle thread timeout is {@code 1} second</LI>
     *     <LI>the default thread factory is used</LI>
     * </UL>
     */
    public static ExecutorService newBlockingFixedThreadPool(int threadCount) {
        return newBlockingFixedThreadPool(threadCount, null);
    }

    /**
     * Creates a new <I>blocking</I> fixed thread pool. Attempts to submit additional tasks will
     * block when the queue is at its maximum size. Threads will automatically exit if idle for
     * more than the timeout period associated with the pool.
     *
     * <P>The following defaults are used:
     * <UL>
     *     <LI>event queue size is {@code threadCount*2}</LI>
     *     <LI>idle thread timeout is {@code 1} second</LI>
     * </UL>
     */
    public static ExecutorService newBlockingFixedThreadPool(int threadCount, ThreadFactory threadFactory) {
        return newBlockingFixedThreadPool(
                threadCount,
                threadCount*2,
                DEFAULT_THREAD_POOL_TIMEOUT_MILLIS,
                TimeUnit.MILLISECONDS,
                threadFactory);
    }

    /**
     * Creates a new <I>blocking</I> fixed thread pool. Attempts to submit additional tasks will
     * block when the queue is at its maximum size. Threads will automatically exit if idle for
     * more than the timeout period associated with the pool.
     *
     * <P>The following defaults are used:
     * <UL>
     *     <LI>idle thread timeout is {@code 1} second</LI>
     *     <LI>the default thread factory is used</LI>
     * </UL>
     */
    public static ExecutorService newBlockingFixedThreadPool(int threadCount, int maxQueueSize) {
        return newBlockingFixedThreadPool(threadCount, maxQueueSize, 1000, TimeUnit.MILLISECONDS, null);
    }

    /**
     * Creates a new <I>blocking</I> fixed thread pool. Attempts to submit additional tasks will
     * block when the queue is at its maximum size. Threads will automatically exit if idle for
     * more than the timeout period associated with the pool.
     */
    public static ExecutorService newBlockingFixedThreadPool(int threadCount, int maxQueueSize, long timeout, TimeUnit timeoutUnit, ThreadFactory threadFactory) {
        final BlockingQueue<Runnable> queue = (maxQueueSize > 0) ?
                new ThreadPoolQueue(maxQueueSize) :
                new LinkedBlockingDeque<>();
        if(threadFactory == null)
            threadFactory = java.util.concurrent.Executors.defaultThreadFactory();
        final ThreadPoolExecutor service = new ThreadPoolExecutor(
                threadCount,
                threadCount,
                timeout,
                timeoutUnit,
                queue,
                threadFactory);
        service.allowCoreThreadTimeOut(true);
        return service;
    }

    /**
     * Creates a new fixed thread pool. Submitted tasks are added to an unbounded queue when all
     * threads are active. Threads will automatically exit if idle for more than the timeout period
     * associated with the pool.
     */
    public static ExecutorService newFixedThreadPool(int threadCount, long timeout, TimeUnit timeoutUnit, ThreadFactory threadFactory) {
        return newBlockingFixedThreadPool(threadCount, 0, timeout, timeoutUnit, threadFactory);
    }

    /**
     * Creates a new fixed thread pool. Submitted tasks are added to an unbounded queue when all
     * threads are active. Threads will automatically exit if idle for more than the timeout period
     * associated with the pool.
     *
     * <P>The following defaults are used:
     * <UL>
     *     <LI>idle thread timeout is {@code 1} second</LI>
     * </UL>
     */
    public static ExecutorService newFixedThreadPool(int threadCount, ThreadFactory threadFactory) {
        return newBlockingFixedThreadPool(threadCount, 0, DEFAULT_THREAD_POOL_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS, threadFactory);
    }

    /**
     * Creates a new fixed thread pool. Submitted tasks are added to an unbounded queue when all
     * threads are active. Threads will automatically exit if idle for more than the timeout period
     * associated with the pool.
     *
     *
     * <P>The following defaults are used:
     * <UL>
     *     <LI>idle thread timeout is {@code 1} second</LI>
     *     <LI>the default thread factory is used</LI>
     * </UL>
     */
    public static ExecutorService newFixedThreadPool(int threadCount) {
        return newFixedThreadPool(threadCount, null);
    }
}
