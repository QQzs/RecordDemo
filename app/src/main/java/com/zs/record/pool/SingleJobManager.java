package com.zs.record.pool;

import androidx.annotation.NonNull;

import java.util.Iterator;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

/**
 * Created by edgardo on 2017/10/25.
 * <p>
 * 线程池管理类，可提交多个runnable，支持优先级队列，仅支持同时执行单个线程
 */
public class SingleJobManager {

    protected static final String TAG = SingleJobManager.class.getSimpleName();

    private static volatile SingleJobManager instance;
    private ExecutorService mSingleThreadExecutor;
    private WeakHashMap<Runnable, Future<?>> mThreads = new WeakHashMap<Runnable, Future<?>>();

    private SingleJobManager() {
        mSingleThreadExecutor = Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(@NonNull Runnable r) {
                Thread t = new Thread(r);
                return t;
            }
        });
    }

    public static SingleJobManager getInstance() {
        if (instance != null) {
            return instance;
        }
        synchronized (SingleJobManager.class) {
            if (instance == null) {
                instance = new SingleJobManager();
            }
        }
        return instance;
    }

    /**
     * 提交runnable任务
     *
     * @param jobs
     */
    public synchronized void submitRunnable(Runnable... jobs) {
        for (Runnable job : jobs) {
            put(job);
        }
    }

    /**
     * 提交任务
     *
     * @param job
     */
    public void put(Runnable job) {
        if (job == null) {
            return;
        }
        Future<?> future = mSingleThreadExecutor.submit(job);
        mThreads.put(job, future);
    }

    /**
     * 移除任务
     *
     * @param job
     */
    public void remove(Runnable job) {
        if (job == null) {
            return;
        }
        Future<?> future = mThreads.remove(job);
        if (future != null) {
            future.cancel(true);
        }
    }

    /**
     * 清除任务
     */
    public synchronized void purge() {
        if (!mThreads.isEmpty()) {
            Set<Runnable> jobs = mThreads.keySet();
            Future<?> future = null;

            Iterator<Runnable> iterator = jobs.iterator();
            while (iterator.hasNext()) {
                Runnable job = iterator.next();
                future = mThreads.get(job);
                if (future != null) {
                    future.cancel(true);
                }
            }
            mThreads.clear();
        }
    }

    /**
     * 关闭线程池
     */
    public synchronized void shutdown() {
        purge();
        mSingleThreadExecutor.shutdown();
    }

}
