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

import java.io.File;
import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.sap.cx.es.samples.filewatch.event.FileEvent;
import com.sap.cx.es.samples.filewatch.event.FileEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.file.StandardWatchEventKinds.*;

/**
 * @author Alexei Liubimov <alexei.liubimov@sap.com>
 * @package com.sap.cx.es.samples.filewatch.event
 * @link http://sap.com/
 * @copyright 2020 SAP
 */
public class FileWatcher implements Runnable {
	private final static Logger LOG = LoggerFactory.getLogger(FileWatcher.class);

	protected List<FileEventListener> listeners = new ArrayList<>();
	protected final File file;

	private boolean isFileWatcher;

	// Activation flag for preventing start an another watcher thread
	private boolean isActive = false;

	public FileWatcher(File file) {
		this.file = file;
		this.isFileWatcher = file.isFile();
	}

	synchronized public void watch() {
		if (file.exists() && !isActive) {
			LOG.debug("Watcher activated on: %s", file.getName());

			WatcherRegister.getRegister().executeInThreadPool(this);
			isActive = true;
		}
	}

	@Override
	public void run() {
		try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
			Path path = Paths.get(file.isDirectory() ? file.getAbsolutePath() : file.getParentFile().getAbsolutePath());

			// Register events
			path.register(watchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);

			// Add WatchService to the register watch services pool
			WatcherRegister.getRegister().addWatchService(watchService);

			boolean poll = true;
			while (poll) {
				poll = pollEvents(watchService);
			}
		} catch (IOException | InterruptedException | ClosedWatchServiceException e) {
			Thread.currentThread().interrupt();
		}
	}

	protected boolean pollEvents(WatchService watchService) throws InterruptedException {
		WatchKey key = watchService.take();
		Path path = (Path) key.watchable();
		key.pollEvents().stream()
				.filter(event -> validateQualifier(path.resolve((Path) event.context()).toFile()))
				.forEach(event ->
					notifyListeners(event.kind(), path.resolve((Path) event.context()).toFile())
				);
		return key.reset();
	}

	protected void notifyListeners(WatchEvent.Kind<?> kind, File file) {
		FileEvent event = new FileEvent(file);

		LOG.debug("Handle file event %s on %s", kind, file.getName());

		// Create unmodifiable list for prevent ConcurrentModificationException
		List<FileEventListener> unmodifiableListeners = Collections.unmodifiableList(listeners);

		if (kind == ENTRY_CREATE) {
			LOG.debug("Created: %s", file);
			unmodifiableListeners.stream()
					.filter(listener -> Objects.nonNull(listener))
					.forEach(listener -> WatcherRegister.getRegister().executeInThreadPool(() -> listener.onCreated(event)));
		} else if (kind == ENTRY_MODIFY) {
			LOG.debug("Modified: %s", file);
			unmodifiableListeners.stream()
					.filter(listener -> Objects.nonNull(listener))
					.forEach(listener -> WatcherRegister.getRegister().executeInThreadPool(() -> listener.onModified(event)));
		} else if (kind == ENTRY_DELETE) {
			LOG.debug("Deleted: %s", file);
			unmodifiableListeners.stream()
					.filter(listener -> Objects.nonNull(listener))
					.forEach(listener -> WatcherRegister.getRegister().executeInThreadPool(() -> listener.onDeleted(event)));
		}
	}

	public FileWatcher addListener(FileEventListener listener) {
		listeners.add(listener);
		return this;
	}

	public FileWatcher removeListener(FileEventListener listener) {
		listeners.remove(listener);
		return this;
	}

	public FileWatcher setListeners(List<FileEventListener> listeners) {
		this.listeners = listeners;
		return this;
	}

	public List<FileEventListener> getListeners() {
		return listeners;
	}

	private boolean validateQualifier(File file){
		return !isFileWatcher || file.getAbsolutePath().equals(this.file.getAbsolutePath());
	}

	public String getWatcherKey(){
		return file.getAbsolutePath();
	}
}
