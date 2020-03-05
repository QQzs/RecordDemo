package com.zs.record.pool;

import android.os.Process;

import com.zs.record.util.CommonLogger;

/**
 * 线程池管理类，可提交runnable，支持优先级队列
 */
public class JobManager extends ThreadPool {
	protected static final String TAG = JobManager.class.getSimpleName();

	private static volatile JobManager instance;

	private static final int CPU_COUNT = Runtime.getRuntime()
			.availableProcessors();
	private static final int CORE_POOL_SIZE = CPU_COUNT + 1;
	private static final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1;
	private static final int KEEP_ALIVE = 1;

	private JobManager() {
		// super(2, MAX_THREAD_COUNT, 3, Thread.NORM_PRIORITY);
		super(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE,
				Process.THREAD_PRIORITY_BACKGROUND);
		CommonLogger.d(TAG, "CPU_COUNT:"+CPU_COUNT);
	}

	public static JobManager getInstance() {
		if (instance != null) {
			return instance;
		}
		synchronized (JobManager.class) {
			if (instance == null) {
				instance = new JobManager();
			}
		}
		return instance;
	}

	/**
	 * 提交runnable任务
	 * @param job
	 */
	public void submitRunnable(Runnable job) {
		put(job);
	}

	/**
	 * 关闭线程池
	 */
	public void shutdownPool() {
		shutdown();
	}
}
