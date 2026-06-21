package org.afterlike.lucid.task;

final class DelayedTask {
	private int ticks;
	private final Runnable runnable;
	DelayedTask(final int ticks, final Runnable runnable) {
		this.ticks = ticks;
		this.runnable = runnable;
	}

	boolean tick() {
		return ticks-- <= 0;
	}

	void run() {
		runnable.run();
	}
}
