package gov.tak.platform.util.concurrent;

import java.util.concurrent.ArrayBlockingQueue;

final class ThreadPoolQueue extends ArrayBlockingQueue<Runnable> {

    public ThreadPoolQueue(int capacity) {
        super(capacity);
    }

    @Override
    public boolean offer(Runnable e) {
        try {
            put(e);
        } catch (InterruptedException e1) {
            Thread.currentThread().interrupt();
            return false;
        }
        return true;
    }

}
