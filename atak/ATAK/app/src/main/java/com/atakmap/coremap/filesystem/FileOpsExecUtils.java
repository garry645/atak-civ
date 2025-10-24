
package com.atakmap.coremap.filesystem;

import com.atakmap.coremap.concurrent.NamedThreadFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Utilities that aid in offloading Disk I/O
 */
public class FileOpsExecUtils {

    private final static String TAG = "FileOptsExecUtils";

    // TODO: tweak for ideal performance
    private final static int MAX_GENERAL_THREAD_COUNT = 1;

    // TODO: tweak for proper balance between performance and memory usage
    private final static int MAX_GENERAL_IO_CAPACITY = Integer.MAX_VALUE;

    private final static ExecutorService generalFileOpsExecutor = new ThreadPoolExecutor(
            1, MAX_GENERAL_THREAD_COUNT,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(MAX_GENERAL_IO_CAPACITY),
            new NamedThreadFactory(TAG));

    /**
     * Executor for offloading general I/O tasks that don't need to be serialized with other I/O
     *
     * @return an executor service for offloading file io tasks
     */
    public static ExecutorService getGeneralFileOpsExecutor() {
        return generalFileOpsExecutor;
    }
}
