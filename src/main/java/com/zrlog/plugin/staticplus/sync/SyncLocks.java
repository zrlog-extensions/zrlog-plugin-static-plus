package com.zrlog.plugin.staticplus.sync;

import java.io.File;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

class SyncLocks {

    private static final Map<String, ReentrantLock> PROCESS_LOCKS = new ConcurrentHashMap<>();

    private SyncLocks() {
    }

    static <T> T withProcessLock(String key, SyncCallable<T> callable) throws Exception {
        ReentrantLock lock = PROCESS_LOCKS.computeIfAbsent(key, ignored -> new ReentrantLock());
        lock.lock();
        try {
            return callable.call();
        } finally {
            lock.unlock();
        }
    }

    static <T> T withProcessAndFileLock(File lockFile, SyncCallable<T> callable) throws Exception {
        return withProcessLock(lockFile.getAbsolutePath(), () -> {
            try (FileChannel lockFileChannel = FileChannel.open(lockFile.toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                 FileLock ignored = lockFileChannel.lock()) {
                return callable.call();
            }
        });
    }

    @FunctionalInterface
    interface SyncCallable<T> {
        T call() throws Exception;
    }
}
