package clicker.clicking;

/** Counts generated clicks and measures the live clicks-per-second over the last second. */
public final class ClickStats {
	private static final long WINDOW_NANOS = 1_000_000_000L;
	private static final int CAPACITY = 512;

	private final long[] times = new long[CAPACITY];
	private int head;
	private int size;
	private long primaryClicks;
	private long secondaryClicks;
	private long lastClickNanos;

	public void record(long nowNanos, boolean secondary) {
		if (secondary) {
			this.secondaryClicks++;
		} else {
			this.primaryClicks++;
		}
		this.lastClickNanos = nowNanos;
		if (this.size == CAPACITY) {
			this.head = (this.head + 1) % CAPACITY;
			this.size--;
		}
		this.times[(this.head + this.size) % CAPACITY] = nowNanos;
		this.size++;
	}

	/** Clicks executed during the last second. */
	public int clicksPerSecond(long nowNanos) {
		while (this.size > 0 && nowNanos - this.times[this.head] > WINDOW_NANOS) {
			this.head = (this.head + 1) % CAPACITY;
			this.size--;
		}
		return this.size;
	}

	public long primaryClicks() {
		return this.primaryClicks;
	}

	public long secondaryClicks() {
		return this.secondaryClicks;
	}

	public long totalClicks() {
		return this.primaryClicks + this.secondaryClicks;
	}

	public long lastClickNanos() {
		return this.lastClickNanos;
	}

	public void reset() {
		this.head = 0;
		this.size = 0;
		this.primaryClicks = 0;
		this.secondaryClicks = 0;
		this.lastClickNanos = 0;
	}
}
