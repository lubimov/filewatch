/**
 * ***********************************************************************
 * Copyright (c) 2020, SAP <sap.com>
 * <p>
 * All portions of the code written by SAP are property of SAP.
 * All Rights Reserved.
 * <p>
 * SAP
 * <p>
 * Moscow, Russian Federation
 * <p>
 * Web: sap.com
 * ***********************************************************************
 */
package com.sap.cx.es.samples.filewatch;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.sun.istack.internal.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import static com.google.common.util.concurrent.MoreExecutors.shutdownAndAwaitTermination;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 *
 *
 * @author Alexei Liubimov <alexei.liubimov@sap.com>
 * @package com.sap.cx.es.samples.filewatch
 * @link http://sap.com/
 * @copyright 2020 SAP
 */
public class WatcherRegister {
	protected static final List<WatchService> watchServices = new ArrayList<>();

	private static final WatcherRegister register = new WatcherRegister();
	private final ConcurrentHashMap<String, FileWatcher> watchers = new ConcurrentHashMap<String, FileWatcher>();
	private final ExecutorService threadPool;

	private WatcherRegister(){
		final ThreadFactory threadFactory = new ThreadFactoryBuilder()
				.setNameFormat("WatcherRegister-%d")
				.setDaemon(true)
				.build();
		threadPool = Executors.newCachedThreadPool(threadFactory);
	}

	protected void executeInThreadPool(Runnable runnable){
		threadPool.execute(runnable);
	}

	public static WatcherRegister getRegister() {
		return register;
	}

	/**
	 * Add WathchService to the global pool
	 * @param watchService
	 */
	protected void addWatchService(WatchService watchService){
		watchServices.add(watchService);
	}

	@NotNull
	public synchronized FileWatcher createWatcher(@NotNull final File file) {
		if (watchers.containsKey(file.getAbsolutePath())) {
			return watchers.get(file.getAbsolutePath());
		}

		FileWatcher watcher = new FileWatcher(file);
		watchers.put(file.getAbsolutePath(), watcher);
		return watcher;
	}

	/**
	 * Use it for destroy WatchRegister under application shutdown process (ServletContextListener.contextDestroyed etc.)
	 */
	public static void stopWatcherRegister() {
		// Stop WatchServices
		closeAllWatchers();

		// Wait listener execution
		shutdownAndAwaitTermination(WatcherRegister.getRegister().threadPool, 10, SECONDS);
	}

	/**
	 * Close all registered WatchServices
	 *
	 * @return
	 */
	public static void closeAllWatchers() {
		// Stop WatchServices
		for (WatchService watchService : WatcherRegister.watchServices){
			try {
				if (Objects.nonNull(watchService)) {
					watchService.close();
				}
			} catch (IOException e) {
				// do nothing
			}
		}
		WatcherRegister.watchServices.clear();

		// Clean watchers
		WatcherRegister instance = WatcherRegister.getRegister();
		instance.watchers.entrySet().stream().forEach(entry -> entry.getValue().getListeners().clear());
		instance.watchers.clear();
	}

}
