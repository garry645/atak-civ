
package com.atakmap.util;

import com.atakmap.android.util.ParallelTrackExecutorService;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class ParallelTrackExecutorServiceTest {

    @Test
    public void simple_submission() {
        boolean[] ran = {
                false
        };
        ParallelTrackExecutorService service = new ParallelTrackExecutorService(
                1);
        service.submit(new Runnable() {
            @Override
            public void run() {
                ran[0] = true;
            }
        }, "uid1");

        shutdownService(service);
        Assert.assertTrue(ran[0]);
    }

    private void shutdownService(ParallelTrackExecutorService service) {
        service.shutdown();
        try {
            Assert.assertTrue(
                    service.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS));
        } catch (InterruptedException e) {
            Assert.fail();
        }
    }

    @Test
    public void flurry_of_unique_submissions() {

        final int COUNT = 200;
        final boolean[] ran = new boolean[COUNT];
        final ParallelTrackExecutorService service = new ParallelTrackExecutorService(
                2);

        for (int i = 0; i < COUNT; ++i) {
            final int index = i;
            service.submit(new Runnable() {
                @Override
                public void run() {
                    ran[index] = true;
                }
            }, "uid" + i);
        }

        shutdownService(service);
        for (int i = 0; i < COUNT; ++i)
            Assert.assertTrue(ran[i]);
    }

    static class Gate {
        public synchronized void waitForStart() {
            try {
                while (!open)
                    wait();
            } catch (InterruptedException e) {
                // let fall through so tests will fail
            }
        }

        public synchronized void start() {
            open = true;
            notifyAll();
        }

        boolean open;
    }

    private Gate submitGate(ParallelTrackExecutorService service) {
        final Gate startGate = new Gate();

        // gate the start
        service.submit(new Runnable() {
            @Override
            public void run() {
                startGate.waitForStart();
            }
        }, "gate");

        return startGate;
    }

    private Future<?> submitTask(ParallelTrackExecutorService service,
            String uid, boolean[] ran, int index) {
        return service.submit(createTask(ran, index), uid);
    }

    private Future<?> submitErrorTask(ParallelTrackExecutorService service,
            String uid, boolean[] ran, int index) {
        return service.submit(new Runnable() {
            @Override
            public void run() {
                ran[index] = true;
                throw new RuntimeException("error");
            }
        });
    }

    private Runnable createTask(boolean[] ran, int index) {
        return new Runnable() {
            @Override
            public void run() {
                ran[index] = true;
            }
        };
    }

    private Callable<Integer> createResultTask(boolean[] ran, int index,
            int result) {
        return new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                ran[index] = true;
                return result;
            }
        };
    }

    private Callable<Integer> createGateResultTask(Gate[] gateOut, int result) {
        Gate gate = new Gate();
        gateOut[0] = gate;
        return new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                gate.waitForStart();
                return result;
            }
        };
    }

    @Test
    public void future_cancel() {

        ParallelTrackExecutorService service = new ParallelTrackExecutorService(
                1);
        final Gate startGate = submitGate(service);

        boolean[] ran = {
                false
        };
        Future<?> f = submitTask(service, "uid", ran, 0);

        Assert.assertFalse(f.isCancelled());
        Assert.assertFalse(f.isDone());
        Assert.assertTrue(f.cancel(false));
        Assert.assertTrue(f.isCancelled());
        Assert.assertTrue(f.isDone());

        startGate.start();

        try {
            f.get();
            Assert.fail();
        } catch (CancellationException e) {
            // expecting this
        } catch (Exception e) {
            Assert.fail();
        }

        shutdownService(service);
        Assert.assertFalse(ran[0]);
    }

    @Test
    public void future_error() {
        ParallelTrackExecutorService service = new ParallelTrackExecutorService(
                1);

        boolean[] ran = {
                false
        };
        Future<?> f = submitErrorTask(service, "uid", ran, 0);

        try {
            f.get();
        } catch (ExecutionException e) {
            // expecting this
        } catch (Throwable t) {
            Assert.fail();
        }

        shutdownService(service);
        Assert.assertTrue(ran[0]);
    }

    @Test
    public void test_invoke_all() {
        ParallelTrackExecutorService service = new ParallelTrackExecutorService(
                1);
        boolean[] ran = {
                false, false, false
        };
        List<Callable<Integer>> tasks = new ArrayList<>();
        tasks.add(createResultTask(ran, 0, 0));
        tasks.add(createResultTask(ran, 1, 1));
        tasks.add(createResultTask(ran, 2, 2));
        try {
            List<Future<Integer>> futures = service.invokeAll(tasks);
            Assert.assertNotNull(futures);
            Assert.assertEquals(futures.size(), 3);
            for (int i = 0; i < 3; ++i) {
                Assert.assertEquals(i, futures.get(i).get().intValue());
            }
        } catch (InterruptedException | ExecutionException e) {
            Assert.fail();
        }
    }

    @Test
    public void test_invoke_all_timeout() {
        ParallelTrackExecutorService service = new ParallelTrackExecutorService(
                1);
        boolean[] ran = {
                false, false
        };
        List<Callable<Integer>> tasks = new ArrayList<>();
        Gate[] gate = new Gate[1];
        tasks.add(createGateResultTask(gate, 0));
        tasks.add(createResultTask(ran, 1, 1));
        tasks.add(createResultTask(ran, 2, 2));
        try {
            List<Future<Integer>> futures = service.invokeAll(tasks, 1,
                    TimeUnit.MILLISECONDS);
            Assert.assertNotNull(futures);
            Assert.assertEquals(futures.size(), tasks.size());

            for (int i = 0; i < futures.size(); ++i) {
                Assert.assertTrue(futures.get(i).isDone());
                Assert.assertTrue(futures.get(i).isCancelled());
                try {
                    futures.get(i).get();
                    Assert.fail();
                } catch (CancellationException e) {
                    // expecting this
                }
            }

        } catch (InterruptedException | ExecutionException e) {
            Assert.fail();
        }

        gate[0].start();

        shutdownService(service);
    }

    @Test
    public void test_invoke_any() {
        ParallelTrackExecutorService service = new ParallelTrackExecutorService(
                2);
        boolean[] ran = {
                false, false
        };
        List<Callable<Integer>> tasks = new ArrayList<>();
        Gate[] gate = new Gate[1];
        tasks.add(createGateResultTask(gate, 0));
        tasks.add(createResultTask(ran, 1, 1));

        try {
            Integer result = service.invokeAny(tasks);
            Assert.assertNotNull(result);
            Assert.assertEquals(result.intValue(), 1);
        } catch (ExecutionException | InterruptedException e) {
            Assert.fail();
        }

        gate[0].start();

        shutdownService(service);
    }
}
