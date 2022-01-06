package com.limpoxe.andsock;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExcutorThread {
    private final String TAG;
    private final ExecutorService THREAD_POOL_EXECUTOR;

    public ExcutorThread(String name) {
        TAG = name;
        THREAD_POOL_EXECUTOR = Executors.newSingleThreadExecutor(new SoThreadFactory(name));
    }

    public void exec(Runnable task) {
        THREAD_POOL_EXECUTOR.submit(() -> {
            try {
                task.run();
            } catch (Exception e) {
                e.printStackTrace();
                LogUtil.log(TAG, "exec task cause exception", e);
            }
            return null;
        });
    }

    public void shutdown() {
        THREAD_POOL_EXECUTOR.shutdown();
    }
}
