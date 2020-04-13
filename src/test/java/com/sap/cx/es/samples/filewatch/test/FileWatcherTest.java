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
package com.sap.cx.es.samples.filewatch.test;

import com.sap.cx.es.samples.filewatch.FileAdapter;
import com.sap.cx.es.samples.filewatch.FileWatcher;
import com.sap.cx.es.samples.filewatch.WatcherRegister;
import com.sap.cx.es.samples.filewatch.event.FileEvent;
import javafx.util.Pair;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

/**
 * @author Alexei Liubimov <alexei.liubimov@sap.com>
 * @package com.sap.cx.es.samples.filewatch.test
 * @link http://sap.com/
 * @copyright 2020 SAP
 */
public class FileWatcherTest {

	/**
	 * Test of catching Create-Modify-Delete events
	 *
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test
	public void testFW_cmdEvents() throws IOException, InterruptedException {
		File folder = new File("src/test/resources");
		final Map<String, String> map = new HashMap<>();

		FileWatcher watcher = new FileWatcher(folder);
		watcher.addListener(new FileAdapter() {
			public void onCreated(FileEvent event) {
				map.put("file.created", event.getFile().getName());
			}

			public void onModified(FileEvent event) {
				map.put("file.modified", event.getFile().getName());
			}

			public void onDeleted(FileEvent event) {
				map.put("file.deleted", event.getFile().getName());
			}
		}).watch();

		assertEquals(1, watcher.getListeners().size());
		Thread.sleep(1000);

		File file = new File(folder + "/test.txt");
		try (FileWriter writer = new FileWriter(file)) {
			writer.write("Some String");
		}
		Thread.sleep(1000);

		file.delete();
		Thread.sleep(1000);

		assertEquals(file.getName(), map.get("file.created"));
		assertEquals(file.getName(), map.get("file.modified"));
		assertEquals(file.getName(), map.get("file.deleted"));
	}

	/**
	 * File modification test
	 *
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test
	public void testFW_mdFileEvents() throws IOException, InterruptedException {
		final Map<String, String> map = new HashMap<>();
		File file = new File("src/test/resources/testFile.txt");
		if (!file.exists()) {
			file.createNewFile();
		}

		FileWatcher watcher = new FileWatcher(file);
		watcher.addListener(new FileAdapter() {
			public void onModified(FileEvent event) {
				map.put("file.modified", event.getFile().getName());
			}

			public void onDeleted(FileEvent event) {
				map.put("file.deleted", event.getFile().getName());
			}
		}).watch();
		Thread.sleep(2000);

		try (FileWriter writer = new FileWriter(file)) {
			writer.write("Some String");
		}
		Thread.sleep(2000);
		assertEquals(file.getName(), map.get("file.modified"));

		file.delete();
		Thread.sleep(2000);
		assertEquals(file.getName(), map.get("file.deleted"));
	}

	/**
	 * Test that long executed listeners are allowed.
	 *
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test
	public void testFW_async() throws IOException, InterruptedException {
		File folder = new File("src/test/resources");
		final Map<String, String> map = new HashMap<>();

		FileWatcher watcher = new FileWatcher(folder);
		watcher.addListener(new FileAdapter() {
			public void onModified(FileEvent event) {
				try {
					Thread.sleep(2000);
					map.put("file.modified", event.getFile().getName());
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}

			public void onDeleted(FileEvent event) {
				try {
					Thread.sleep(2000);
					map.put("file.deleted", event.getFile().getName());
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		}).watch();

		assertEquals(1, watcher.getListeners().size());
		Thread.sleep(1000);

		File file = new File(folder + "/test.txt");
		try (FileWriter writer = new FileWriter(file)) {
			writer.write("Some String");
		}
		Thread.sleep(1000);
		assertNull(map.get("file.modified"));
		Thread.sleep(2000);
		assertEquals(file.getName(), map.get("file.modified"));

		file.delete();
		Thread.sleep(1000);
		assertNull(map.get("file.deleted"));
		Thread.sleep(2000);
		assertEquals(file.getName(), map.get("file.deleted"));
	}

	/**
	 * Folder modification test.
	 *
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test
	public void testWR_folderModification() throws IOException, InterruptedException {
		File folder = new File("src/test/resources");
		final Map<String, String> map = new HashMap<>();

		FileWatcher watcher = WatcherRegister.getRegister().createWatcher(folder);
		watcher.addListener(new FileAdapter() {
			public void onCreated(FileEvent event) {
				map.put("file.created", event.getFile().getName());
			}

			public void onModified(FileEvent event) {
				map.put("file.modified", event.getFile().getName());
			}

			public void onDeleted(FileEvent event) {
				map.put("file.deleted", event.getFile().getName());
			}
		}).watch();

		assertEquals(1, watcher.getListeners().size());
		Thread.sleep(3000);

		File file = new File(folder + "/test.txt");
		try (FileWriter writer = new FileWriter(file)) {
			writer.write("Some String");
		}
		Thread.sleep(3000);

		file.delete();
		Thread.sleep(3000);

		assertEquals(file.getName(), map.get("file.created"));
		assertEquals(file.getName(), map.get("file.modified"));
		assertEquals(file.getName(), map.get("file.deleted"));
	}

	/**
	 * In case of registering several watchers on one file should use single watcher with a list of listeners.
	 *
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test
	public void testWR_watchersCount() throws IOException, InterruptedException {
		File folder = new File("src/test/resources");

		FileWatcher watcher = WatcherRegister.getRegister().createWatcher(folder);
		watcher.addListener(new FileAdapter() {
			public void onModified(FileEvent event) {
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		}).watch();

		assertEquals(1, watcher.getListeners().size());

		// Second watcher for the same folder
		File folder2 = new File("src/test/resources");
		FileWatcher watcher2 = WatcherRegister.getRegister().createWatcher(folder2);
		watcher2.addListener(new FileAdapter() {
			public void onModified(FileEvent event) {
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		}).watch();

		assertSame(watcher, watcher2);
		assertEquals(2, watcher2.getListeners().size());
	}

	/**
	 * Volume test. One watcher on each of 1000 files.
	 * A timeout of notification should be less than 3 seconds.
	 *
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test
	public void testWR_1000watchers() throws IOException, InterruptedException {
		File folder = new File("src/test/resources");

		Map<String, Pair<AtomicLong, AtomicLong>> map = new ConcurrentHashMap<String, Pair<AtomicLong, AtomicLong>>();
		int N = 1000;
		long VALID_DURATION_MS = 3000;
		List<File> files = new ArrayList<>(N);

		// Create N files
		for (int i = 0; i < N; ++i) {
			File file = new File(folder + "/test" + i + ".txt");
			if (!file.exists()) {
				file.createNewFile();
			}
			files.add(file);
		}
		Thread.sleep(N * 2);

		// Register watchers on files
		for (File file : files) {
			// Watcher on the file
			WatcherRegister.getRegister().createWatcher(file)
					.addListener(new FileAdapter() {
						public void onModified(FileEvent event) {
							// Register time for the first EVENT_MODIFY event
							map.get(event.getFile().getAbsolutePath()).getValue().compareAndSet(0, System.currentTimeMillis());
						}
					}).watch();
			map.put(file.getAbsolutePath(), new Pair<>(new AtomicLong(0), new AtomicLong(0)));
		}

		try {
			Thread.sleep(1000);

			// Modify files
			for (File file : files) {
				try (FileWriter writer = new FileWriter(file)) {
					// Register start modification time
					writer.write("Some String");
					map.get(file.getAbsolutePath()).getKey().set(System.currentTimeMillis());
				}
			}
			Thread.sleep(N * 2);

			// Check the number of executions
			// Events count on the parent folder. It should be the same as sum of all internal files event count
			for (File file : files) {
				Pair<AtomicLong, AtomicLong> timePair = map.get(file.getAbsolutePath());
				long duration = timePair.getValue().get() - timePair.getKey().get();

				// Check that duration less then VALID_DURATION_MS sec
				//System.out.printf("File: %s; Duration, ms: %s\n", file.getName(), durationNano);
				assertTrue("File " + file.getName() + "; Zero Duration", timePair.getValue().get() > 0);
				assertTrue("File " + file.getName() + "; Duration, ms: " + duration, duration < VALID_DURATION_MS);
			}

		} finally {
			// Delete all files
			for (File file : files) {
				file.delete();
			}
		}
	}

	/**
	 * Volume test. 1000 listener on the single file.
	 * A timeout of notification should be less than 1 seconds.
	 *
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test
	public void testWR_1000listeners() throws IOException, InterruptedException {
		File folder = new File("src/test/resources");

		Map<String, Pair<AtomicLong, AtomicLong>> map = new ConcurrentHashMap<String, Pair<AtomicLong, AtomicLong>>();
		int N = 1000;
		long VALID_DURATION_MS = 1000;

		// Create file
		File file = new File(folder + "/test1000L.txt");
		if (!file.exists()) {
			file.createNewFile();
		}
		Thread.sleep(1000);

		// Register watchers on files
		FileWatcher lastWatcher = null;
		for (int i = 0; i < N; ++i) {
			final int idx = i;
			lastWatcher = WatcherRegister.getRegister().createWatcher(file)
					.addListener(new FileAdapter() {
						public void onModified(FileEvent event) {
							try {
								// Register time for the first EVENT_MODIFY event
								map.get(event.getFile().getAbsolutePath() + idx).getValue().compareAndSet(0, System.currentTimeMillis());
								Thread.sleep(2000);
							} catch (InterruptedException e) {
								Thread.currentThread().interrupt();
							}
						}
					});
			lastWatcher.watch();
			map.put(file.getAbsolutePath() + idx, new Pair<>(new AtomicLong(0), new AtomicLong(0)));
		}
		assertEquals("Watcher listeners count", N, lastWatcher.getListeners().size());

		try {
			Thread.sleep(2000);

			// Modify file
			try (FileWriter writer = new FileWriter(file)) {
				// Register start modification time
				long currentTime = System.currentTimeMillis();
				for (int i = 0; i < N; ++i) {
					map.get(file.getAbsolutePath() + i).getKey().set(currentTime);
				}
				writer.write("Some String");
			}
			Thread.sleep(5000);

			// Check the number of executions
			// Events count on the parent folder. It should be the same as sum of all internal files event count
			for (int i = 0; i < N; ++i) {
				String key = file.getAbsolutePath() + i;
				Pair<AtomicLong, AtomicLong> timePair = map.get(key);
				long duration = timePair.getValue().get() - timePair.getKey().get();

				// Check that duration less then VALID_DURATION_MS sec
				//System.out.printf("Listener: %s; Duration, ms: %s\n", key, duration);
				assertTrue("Listener " + key + "; Zero Duration", timePair.getValue().get() > 0);
				assertTrue("Listener " + key + "; Duration, ms: " + duration, duration < VALID_DURATION_MS);
			}

		} finally {
			// Delete file
			file.delete();
		}
	}

	@After
	public void closeAllWatchers() throws InterruptedException {
		Thread.sleep(1000);
		WatcherRegister.closeAllWatchers();
	}

	@AfterClass
	public static void shutdownRegister() {
		WatcherRegister.stopWatcherRegister();
	}
}
