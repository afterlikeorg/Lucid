package org.afterlike.lucid.task;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import org.afterlike.lucid.event.api.EventPhase;
import org.afterlike.lucid.event.impl.client.GameTickEvent;
import re.tsuku.fastbus.Subscribe;

public final class DelayedTaskHandler {
	private static final DelayedTaskHandler INSTANCE = new DelayedTaskHandler();
	private final List<DelayedTask> tasks = new ArrayList<>();
	private final List<DelayedTask> pendingTasks = new ArrayList<>();
	private boolean ticking;
	private DelayedTaskHandler() {
	}

	public static DelayedTaskHandler get() {
		return INSTANCE;
	}

	public static void schedule(final int ticks, final Runnable runnable) {
		INSTANCE.scheduleTask(ticks, runnable);
	}

	@Subscribe
	private void onTick(final GameTickEvent event) {
		if (event.getPhase() != EventPhase.PRE) {
			return;
		}
		ticking = true;
		try {
			final Iterator<DelayedTask> iterator = tasks.iterator();
			while (iterator.hasNext()) {
				final DelayedTask task = iterator.next();
				if (task.tick()) {
					iterator.remove();
					task.run();
				}
			}
		} finally {
			ticking = false;
			flushPendingTasks();
		}
	}

	private void scheduleTask(final int ticks, final Runnable runnable) {
		if (ticks < 0) {
			throw new IllegalArgumentException("ticks must be non-negative");
		}
		final DelayedTask task = new DelayedTask(ticks,
				Objects.requireNonNull(runnable, "runnable"));
		if (ticking) {
			pendingTasks.add(task);
			return;
		}
		tasks.add(task);
	}

	private void flushPendingTasks() {
		if (pendingTasks.isEmpty()) {
			return;
		}
		tasks.addAll(pendingTasks);
		pendingTasks.clear();
	}
}
