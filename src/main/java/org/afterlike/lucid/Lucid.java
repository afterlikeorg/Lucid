package org.afterlike.lucid;

import java.util.Objects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import re.tsuku.fastbus.FastBus;

public class Lucid {
	private static final Logger LOGGER = LogManager.getLogger();
	private static final @Nullable Lucid INSTANCE = new Lucid();
	private static final @NotNull String VERSION = org.afterlike.lucid.BuildConstants.VERSION;
	private final FastBus eventBus;
	public Lucid() {
		this.eventBus = new FastBus();
	}

	public void initialize() {
		final long startTime = System.nanoTime();
		// initialization logic
		LOGGER.info("Initialized in {}ms.", (System.nanoTime() - startTime) / 1_000_000);
	}

	public void lateInitialize() {
		// late initialization logic
	}

	public static @NotNull Lucid get() {
		return Objects.requireNonNull(INSTANCE);
	}

	public FastBus getEventBus() {
		return eventBus;
	}

	public @NotNull String getVersion() {
		return VERSION;
	}
}