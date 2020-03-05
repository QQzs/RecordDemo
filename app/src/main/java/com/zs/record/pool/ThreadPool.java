package com.zs.record.pool;

import android.os.Build;

import java.util.Iterator;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 线程池基类
 */
public abstract class ThreadPool {
	protected static final String TAG = ThreadPool.class.getSimpleName();
	private WeakHashMap<Runnable, Future<?>> mThreads = new WeakHashMap<Runnable, Future<?>>();
	private final PriorityBlockingQueue<Runnable> mPriorityBlockQueue = new PriorityBlockingQueue<Runnable>(128);
	private XThreadPoolExecutor mPool;

	@SuppressWarnings("unused")
	private ThreadPool() {
	}

	public ThreadPool(int corePoolSize, int maximumPoolSize,
			long keepAliveTime, final int threadPriority) {
		this(corePoolSize, maximumPoolSize, false, keepAliveTime,
				threadPriority);
	}

	public ThreadPool(int corePoolSize, int maximumPoolSize,
			boolean allowCoreThreadTimeOut, long keepAliveTime,
			final int threadPriority) {
		mPool = new XThreadPoolExecutor(corePoolSize, maximumPoolSize,
				keepAliveTime, TimeUnit.SECONDS, mPriorityBlockQueue,
				new ThreadFactory() {

					@Override
					public Thread newThread(Runnable r) {
						Thread t = new Thread(r);
						t.setPriority(threadPriority);
						return t;
					}
				}, new RejectedExecutionHandler() {

					@Override
					public void rejectedExecution(Runnable r,
							ThreadPoolExecutor executor) {
						executor.remove(r);
					}
				});
		if (Build.VERSION.SDK_INT >= 9){
			mPool.allowCoreThreadTimeOut(allowCoreThreadTimeOut);
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
		Future<?> future = mPool.submit(job);
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
		mPool.remove(job);
	}

	/**
	 * 清除任务
	 */
	public synchronized void purge() {
		mPool.purge();
		mPriorityBlockQueue.clear();
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
	public void shutdown() {
		purge();
		mPool.shutdown();
	}

}
