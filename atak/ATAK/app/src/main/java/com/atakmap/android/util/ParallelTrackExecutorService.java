
package com.atakmap.android.util;

import android.os.Build;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;

/**
 * An ExecutorService with "tracks" defined by "track IDs". Any Runnable or Callable<V> submitted
 * with a given track ID is guaranteed to be run serially with other tasks submitted with the same
 * track ID. The exact thread and queue used for a given track ID is an implementation detail and
 * can will change depending on resource allocation.
 */
public class ParallelTrackExecutorService implements ExecutorService {

    /**
     * Create a executor given the maximum parallel thread limit
     *
     * @param parallelLimit Maximum number of threads (must be >= 1)
     */
    public ParallelTrackExecutorService(int parallelLimit) {
        this(parallelLimit, 60, TimeUnit.SECONDS,
                Executors.defaultThreadFactory());
    }

    /**
     *
     * @param parallelLimit the limit for the service, must be greater than or equal to 1
     * @param threadTimeout the timeout for the thread
     * @param threadTimeoutUnit the time unit to use
     * @param threadFactory the thread factory used during construction of the threads
     */
    public ParallelTrackExecutorService(int parallelLimit, long threadTimeout,
            TimeUnit threadTimeoutUnit,
            ThreadFactory threadFactory) {

        if (parallelLimit <= 0)
            throw new IllegalArgumentException("parallelLimit must be >= 1");

        this.threadTimeout = threadTimeout;
        this.threadTimeoutUnit = threadTimeoutUnit;
        this.threadFactory = threadFactory;
        this.queues = new ArrayList<>(parallelLimit);

        // add queues upfront, so that allocateQueue() does not need any synchronization
        for (int c = 0; c < parallelLimit; ++c) {
            queues.add(new Queue());
        }
    }

    /**
     * Submit a runnable along a given track
     *
     * @param runnable the runnable task
     * @param trackId the track id
     *
     * @return Future to control cancellation and waiting for completion
     */
    public Future<?> submit(@NonNull Runnable runnable,
            @NonNull String trackId) {

        final Pending<?>[] result = {
                null
        };

        if (!shutdownFlag) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                trackedTasks.compute(trackId,
                        new BiFunction<String, Pending<?>, Pending<?>>() {
                            @Override
                            public Pending<?> apply(String key,
                                    Pending<?> existing) {
                                return result[0] = new Pending<Void>(
                                        existing != null ? existing.queue
                                                : allocateQueue(),
                                        runnable, trackId, null);
                            }
                        });
            } else {
                synchronized (trackedTasks) {
                    Pending<?> existing = trackedTasks.get(trackId);
                    trackedTasks.put(trackId,
                            result[0] = new Pending<Void>(
                                    existing != null ? existing.queue
                                            : allocateQueue(),
                                    runnable, trackId, null));
                }
            }

            if (result[0] != null && result[0].queue != null)
                result[0].queue.submit(result[0]);
        }

        return result[0];
    }

    @Override
    public void execute(Runnable runnable) {
        submit(runnable);
    }

    @Override
    public void shutdown() {
        for (Queue queue : shutdownQueues()) {
            queue.shutdown(false);
        }
    }

    @Override
    public List<Runnable> shutdownNow() {
        List<Runnable> result = new ArrayList<>();
        for (Queue queue : shutdownQueues()) {
            result.addAll(queue.shutdown(true));
        }
        return result;
    }

    @Override
    public boolean isShutdown() {
        shutdownLock.lock();
        try {
            return shutdownFlag;
        } finally {
            shutdownLock.unlock();
        }
    }

    @Override
    public boolean isTerminated() {
        boolean tFlagValue;
        shutdownLock.lock();
        try {
            tFlagValue = terminatedFlag;
        } finally {
            shutdownLock.unlock();
        }
        try {
            return tFlagValue || awaitTermination(0, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            return false;
        }
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit timeUnit)
            throws InterruptedException {

        shutdownLock.lock();
        try {
            if (terminatedFlag) {
                return true;
            }
        } finally {
            shutdownLock.unlock();
        }

        long waitTime = 0;
        final long beginNanos = System.nanoTime();

        for (Queue queue : queues) {
            boolean queueJoined = false;
            while (waitTime < timeout && !queueJoined) {
                // nanos will be Long.MAX_VALUE if it would positively overflow
                final long nanos = TimeUnit.NANOSECONDS
                        .convert(timeout - waitTime, timeUnit);
                if (!(queueJoined = queue.awaitTermination(nanos))) {
                    waitTime = timeUnit.convert(System.nanoTime() - beginNanos,
                            TimeUnit.NANOSECONDS);
                    if (waitTime >= timeout)
                        return false;
                }
            }
        }

        shutdownLock.lock();
        terminatedFlag = true;
        shutdownLock.unlock();
        return true;
    }

    @Override
    public <T> Future<T> submit(Callable<T> callable) {
        Pending<T> result = new Pending<>(allocateQueue(), callable, null);
        if (result.queue != null) {
            result.queue.submit(result);
            return result;
        }
        return null;
    }

    /**
     * Submit a Callable on a given track
     *
     * @param callable the callable
     * @param trackId the track ID
     *
     * @return Future for cancellation control and result fetching
     *
     * @param <T> callable result type
     */
    public <T> Future<T> submit(Callable<T> callable, String trackId) {

        final Pending<T>[] result = new Pending[] {
                null
        };

        if (!shutdownFlag) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                trackedTasks.compute(trackId,
                        new BiFunction<String, Pending<?>, Pending<?>>() {
                            @Override
                            public Pending<?> apply(String key,
                                    Pending<?> existing) {
                                return result[0] = new Pending<>(
                                        existing != null ? existing.queue
                                                : allocateQueue(),
                                        callable, trackId);
                            }
                        });
            } else {
                synchronized (trackedTasks) {
                    Pending<?> existing = trackedTasks.get(trackId);
                    trackedTasks.put(trackId,
                            new Pending<>(
                                    existing != null ? existing.queue
                                            : allocateQueue(),
                                    callable, trackId));
                }
            }

            if (result[0] != null && result[0].queue != null)
                result[0].queue.submit(result[0]);
        }

        return result[0];
    }

    @Override
    public <T> Future<T> submit(Runnable runnable, T result) {
        Pending<T> ret = new Pending<>(allocateQueue(), runnable, null, result);
        if (ret.queue != null) {
            ret.queue.submit(ret);
            return ret;
        }
        return null;
    }

    @Override
    public Future<?> submit(@NonNull Runnable runnable) {
        Pending<Void> result = new Pending<>(allocateQueue(), runnable, null,
                null);
        if (result.queue != null) {
            result.queue.submit(result);
            return result;
        }
        return null;
    }

    @Override
    public <T> List<Future<T>> invokeAll(
            Collection<? extends Callable<T>> collection)
            throws InterruptedException {
        List<Future<T>> result = new ArrayList<>();
        for (Callable<T> callable : collection) {
            result.add(submit(callable));
        }
        for (Future<T> f : result) {
            try {
                f.get();
            } catch (ExecutionException | CancellationException e) {
                // to be discovered by calling code
            }
        }
        return result;
    }

    @Override
    public <T> List<Future<T>> invokeAll(
            Collection<? extends Callable<T>> collection, long timeout,
            TimeUnit timeUnit) throws InterruptedException {

        List<Future<T>> result = new ArrayList<>(collection.size());
        for (Callable<T> c : collection) {
            result.add(submit(c));
        }

        long begin = System.nanoTime();
        long waitTime = 0;
        int index = 0;

        while (waitTime < timeout) {
            try {
                result.get(index).get(timeout - waitTime, timeUnit);
            } catch (ExecutionException | CancellationException e) {
                // to be discovered by calling code
            } catch (TimeoutException e) {
                break;
            }

            waitTime = timeUnit.convert(System.nanoTime() - begin,
                    TimeUnit.NANOSECONDS);
            ++index;
        }

        // cancel remaining (as is called for)
        for (; index < result.size(); ++index) {
            result.get(index).cancel(false);
        }

        return result;
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> collection)
            throws ExecutionException, InterruptedException {

        CompletionService<T> completionService = new ExecutorCompletionService<>(
                this);
        List<Future<T>> futures = new ArrayList<>(collection.size());
        for (Callable<T> callable : collection) {
            futures.add(completionService.submit(callable));
        }

        int num = 0;
        Future<T> done = null;
        T result = null;

        while (num < collection.size()) {
            Future<T> f = completionService.take();
            try {
                result = f.get();
                done = f;
                break;
            } catch (CancellationException | ExecutionException e) {
                // skip over
            }

            num++;
        }

        for (Future<T> f : futures) {
            if (f != done)
                f.cancel(false);
        }

        return result;
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> collection,
            long timeout, TimeUnit timeUnit)
            throws ExecutionException, InterruptedException, TimeoutException {

        CompletionService<T> completionService = new ExecutorCompletionService<>(
                this);
        List<Future<T>> futures = new ArrayList<>(collection.size());
        for (Callable<T> callable : collection) {
            futures.add(completionService.submit(callable));
        }

        int num = 0;
        long begin = System.nanoTime();
        long waitTime = 0;
        Future<T> done = null;
        T result = null;

        while (num < collection.size()) {
            Future<T> f = completionService.poll(timeout - waitTime, timeUnit);
            try {
                result = f.get();
                done = f;
                break;
            } catch (CancellationException | ExecutionException e) {
                // skip over
            }

            waitTime = TimeUnit.NANOSECONDS.convert(System.nanoTime() - begin,
                    timeUnit);
            if (waitTime >= timeout) {
                throw new TimeoutException();
            }

            num++;
        }

        for (Future<T> f : futures) {
            if (f != done)
                f.cancel(false);
        }

        return result;
    }

    private static final String TAG = "ParallelTrackExecutorService";
    private final ThreadFactory threadFactory;
    private final long threadTimeout;
    private final TimeUnit threadTimeoutUnit;
    // >= API24 takes advantage of new compute() API and doesn't need locking, just a ConcurrentHashMap
    private final Map<String, Pending<?>> trackedTasks = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
            ? new ConcurrentHashMap<>()
            : new HashMap<>();
    private final List<Queue> queues;

    // BEGIN: terminateLock protected section
    private final ReentrantLock shutdownLock = new ReentrantLock();
    private volatile boolean shutdownFlag;
    private volatile boolean terminatedFlag;
    // END: queueStateLock protected section

    private static class Pending<T> implements Future<T> {

        Pending(Queue queue, Runnable task, String trackId, T result) {
            this.queue = queue;
            this.task = task;
            this.trackId = trackId;
            this.result = result;
        }

        Pending(Queue queue, Callable<T> callable, String trackId) {
            // can't call this() since we need to allocate runnable with reference to result
            this.queue = queue;
            this.result = null;
            this.trackId = trackId;
            this.task = new Runnable() {
                @Override
                public void run() {
                    try {
                        result = callable.call();
                    } catch (Exception e) {
                        //?
                    }
                }
            };
        }

        final Queue queue;
        final String trackId;
        final Runnable task;
        Pending<?> next;
        T result;
        Throwable error;
        static final int INIT = 0;

        static final int RUNNING = 1;
        static final int DONE = 2;
        static final int CANCELLED = 3;

        int state;

        @Override
        public synchronized boolean cancel(boolean b) {
            if (state == INIT || state == RUNNING) {
                state = CANCELLED;
                return true;
            }
            return false;
        }

        @Override
        public synchronized boolean isCancelled() {
            return state == CANCELLED;
        }

        @Override
        public synchronized boolean isDone() {
            return state != INIT && state != RUNNING;
        }

        @Override
        public synchronized T get()
                throws ExecutionException, InterruptedException {
            while (state == INIT || state == RUNNING)
                this.wait();
            if (state == CANCELLED)
                throw new CancellationException();
            else if (error != null)
                throw new ExecutionException(error);
            return result;
        }

        @Override
        public synchronized T get(long timeout, TimeUnit timeUnit)
                throws ExecutionException, InterruptedException,
                TimeoutException {

            long waitTime = 0;
            final long beginNanos = System.nanoTime();

            while (state == INIT || state == RUNNING) {

                // nanos will be Long.MAX_VALUE if it would positively overflow
                final long nanos = TimeUnit.NANOSECONDS
                        .convert(timeout - waitTime, timeUnit);
                this.wait(nanos / 1000000, (int) (nanos % 1000000));

                if (state == INIT || state == RUNNING) {
                    waitTime = timeUnit.convert(System.nanoTime() - beginNanos,
                            TimeUnit.NANOSECONDS);
                    if (waitTime >= timeout)
                        throw new TimeoutException();
                }
            }

            if (state == CANCELLED)
                throw new CancellationException();
            else if (error != null)
                throw new ExecutionException(error);

            return result;
        }

        public void run() {

            boolean doRun = false;
            synchronized (this) {
                if (state == INIT) {
                    doRun = true;
                    state = RUNNING;
                }
            }

            if (doRun) {
                try {
                    task.run();
                } catch (Throwable t) {
                    // nothing checks this until DONE, so we have exclusive access here
                    error = t;
                } finally {
                    synchronized (this) {
                        if (state == RUNNING) {
                            state = DONE;
                            notifyAll();
                        }
                    }
                }
            }
        }
    }

    private class Queue {

        final ReentrantLock lock = new ReentrantLock();
        final Condition wakeUp = lock.newCondition();
        Pending<?> front;
        Pending<?> back;
        int count;
        volatile boolean shouldTerminate;
        Thread thread;

        void startThread() {
            thread = threadFactory.newThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        runImpl();
                    } finally {
                        // clear out thread to signal spawning new
                        lock.lock();
                        thread = null;
                        lock.unlock();
                    }
                }

                private void runImpl() {

                    boolean running = true;
                    boolean terminate = false;

                    do {
                        try {
                            // wait for next task, terminate signal or timeout
                            final Pending<?> pending = !terminate ? take()
                                    : null;

                            if (pending == null) {
                                running = false;

                                // no need to lock here since no submissions are possible, on
                                // shutdown, these will be taken on shutdownNow()
                                for (Pending<?> walk = front; walk != null; walk = walk.next) {
                                    handlePending(walk);
                                }
                            } else {
                                handlePending(pending);
                            }

                        } catch (InterruptedException e) {
                            terminate = true;
                        }
                    } while (running);
                }
            });

            thread.start();
        }

        void submit(Pending<?> pending) {
            lock.lock();
            try {
                if (!shouldTerminate) {

                    // queue up item
                    pending.next = null; // redundant for new nodes, but just in case
                    if (back != null)
                        back.next = pending;
                    back = pending;
                    if (front == null)
                        front = pending;
                    ++count;

                    if (thread == null) {
                        startThread();
                    }

                    wakeUp.signal();
                }
            } finally {
                lock.unlock();
            }
        }

        List<Runnable> shutdown(boolean takeRemaining) {
            Pending<?> remaining = null;
            int remainingCount = 0;
            lock.lock();
            try {
                shouldTerminate = true;
                wakeUp.signal();
                if (takeRemaining) {
                    remaining = front;
                    remainingCount = count;
                    front = null;
                    back = null;
                    count = 0;
                }
            } finally {
                lock.unlock();
            }

            List<Runnable> result = new ArrayList<>(remainingCount);
            for (Pending<?> walk = remaining; walk != null; walk = walk.next) {
                result.add(walk.task);
            }

            return result;
        }

        Pending<?> take() throws InterruptedException {
            Pending<?> result = null;
            lock.lock();
            try {
                if (!shouldTerminate) {
                    if (count > 0
                            || (wakeUp.await(threadTimeout, threadTimeoutUnit)
                                    && !shouldTerminate)) {
                        result = front;
                        front = front.next;
                        if (front == null)
                            back = null;
                        --count;
                    }
                }
            } finally {
                lock.unlock();
            }
            return result;
        }

        boolean awaitTermination(long nanos) throws InterruptedException {
            Thread joinThread = null;
            lock.lock();
            try {
                if (thread != null)
                    joinThread = thread;
            } finally {
                lock.unlock();
            }
            if (joinThread != null) {
                long millis = nanos / 1000000;
                int remainingNanos = (int) (nanos % 100000);
                joinThread.join(millis, remainingNanos);
                return !joinThread.isAlive();
            }
            return true;
        }
    }

    private void handlePending(Pending<?> pending) {
        pending.run();
        if (pending.trackId != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                trackedTasks.remove(pending.trackId, pending);
            } else {
                synchronized (trackedTasks) {
                    if (trackedTasks.get(pending.trackId) == pending)
                        trackedTasks.remove(pending.trackId);
                }
            }
        }
    }

    private Queue allocateQueue() {
        Queue result = null;

        int minLoadFactor = Integer.MAX_VALUE;
        for (Queue queue : queues) {

            // not locking here, but we're just using it for allocation, so go with it
            int lf = queue.count;

            if (lf < minLoadFactor) {
                result = queue;
                minLoadFactor = lf;
            }
        }

        return result;
    }

    private List<Queue> shutdownQueues() {
        List<Queue> shutdownQueues = Collections.emptyList();
        shutdownLock.lock();
        try {
            if (!shutdownFlag) {
                shutdownQueues = new ArrayList<>(queues);
                shutdownFlag = true;
            }
        } finally {
            shutdownLock.unlock();
        }
        return shutdownQueues;
    }
}
