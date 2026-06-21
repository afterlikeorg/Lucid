package org.afterlike.lucid;

import java.util.Objects;
import org.afterlike.lucid.task.DelayedTaskHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import re.tsuku.fastbus.FastBus;

public class Lucid {
	private static final Logger LOGGER = LogManager.getLogger();
	private static final Lucid INSTANCE = new Lucid();
	private static final String VERSION = org.afterlike.lucid.BuildConstants.VERSION;
	private final FastBus eventBus;
	public Lucid() {
		this.eventBus = new FastBus();
	}

	public void initialize() {
		final long startTime = System.nanoTime();
		eventBus.subscribe(DelayedTaskHandler.get());
		LOGGER.info("Initialized in {}ms.", (System.nanoTime() - startTime) / 1_000_000);
	}

	public void lateInitialize() {
		// late initialization logic
	}

	public static Lucid get() {
		return Objects.requireNonNull(INSTANCE);
	}

	public FastBus getEventBus() {
		return eventBus;
	}

	public String getVersion() {
		return VERSION;
	}
}
