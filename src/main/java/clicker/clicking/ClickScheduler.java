package clicker.clicking;

/**
 * Frame-rate independent click timing for one clicker side.
 *
 * <p>The scheduler is a pure state machine driven by {@link System#nanoTime()} values passed in by the caller,
 * so it never blocks, never starts threads and can be unit tested without Minecraft.
 *
 * <p>Clicks are spaced exactly {@code interval} apart. If a frame arrives late, every click that became due is
 * reported (up to {@link #MAX_CLICKS_PER_FRAME}); after a real lag spike the backlog is dropped instead of being
 * released as a burst.
 */
public final class ClickScheduler {
	/** Hard cap on how many clicks a single frame may release. */
	public static final int MAX_CLICKS_PER_FRAME = 10;
	/** If the schedule falls further behind than this, it resynchronises rather than catching up. */
	public static final long MAX_BACKLOG_NANOS = 250_000_000L;

	private boolean running;
	private long nextClickNanos;

	/**
	 * Starts (or restarts) the schedule.
	 *
	 * @param delayFirstClick when true the first click happens one interval from now. Hold mode uses this because the
	 *                        physical button press that started holding already produced a vanilla click.
	 */
	public void start(long nowNanos, long intervalNanos, boolean delayFirstClick) {
		this.running = true;
		this.nextClickNanos = delayFirstClick ? nowNanos + intervalNanos : nowNanos;
	}

	public void stop() {
		this.running = false;
	}

	public boolean isRunning() {
		return this.running;
	}

	public long nextClickNanos() {
		return this.nextClickNanos;
	}

	/**
	 * Releases every click that is due at {@code nowNanos}.
	 *
	 * @param scheduledTimes receives the scheduled timestamp of each released click (length at least
	 *                       {@link #MAX_CLICKS_PER_FRAME})
	 * @return how many clicks are due this frame
	 */
	public int poll(long nowNanos, long intervalNanos, long[] scheduledTimes) {
		if (!this.running || intervalNanos <= 0) {
			return 0;
		}

		// The CPS was raised while waiting: never wait longer than one (new) interval.
		if (this.nextClickNanos - nowNanos > intervalNanos) {
			this.nextClickNanos = nowNanos + intervalNanos;
		}

		// A lag spike (or the game was frozen): skip the backlog, click once now and continue from here.
		if (nowNanos - this.nextClickNanos > MAX_BACKLOG_NANOS) {
			this.nextClickNanos = nowNanos;
		}

		int due = 0;
		while (nowNanos >= this.nextClickNanos && due < MAX_CLICKS_PER_FRAME) {
			scheduledTimes[due++] = this.nextClickNanos;
			this.nextClickNanos += intervalNanos;
		}

		// Still behind after releasing the per-frame maximum: resynchronise instead of bursting next frame.
		if (due == MAX_CLICKS_PER_FRAME && nowNanos >= this.nextClickNanos) {
			this.nextClickNanos = nowNanos + intervalNanos;
		}
		return due;
	}

	/** Converts clicks per second to the nanosecond spacing between clicks. */
	public static long intervalNanos(int clicksPerSecond) {
		int cps = Math.max(1, clicksPerSecond);
		return 1_000_000_000L / cps;
	}
}
