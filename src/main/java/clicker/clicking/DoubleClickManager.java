package clicker.clicking;

/**
 * Holds the pending second click of each generated double click.
 *
 * <p>Only primary (scheduled) clicks may enqueue a second click; second clicks are executed directly and can never
 * enqueue anything themselves, so double clicking cannot loop or recurse. The queue is a fixed-size ring buffer:
 * if it is ever full, new second clicks are dropped instead of growing memory.
 */
public final class DoubleClickManager {
	public static final int CAPACITY = 64;

	private final long[] dueNanos = new long[CAPACITY];
	private int head;
	private int size;

	/** Queues a second click. Returns false if the queue is full and the click was dropped. */
	public boolean schedule(long dueAtNanos) {
		if (this.size == CAPACITY) {
			return false;
		}
		this.dueNanos[(this.head + this.size) % CAPACITY] = dueAtNanos;
		this.size++;
		return true;
	}

	/** Removes and counts every queued second click that is due at {@code nowNanos}. */
	public int drainDue(long nowNanos) {
		int drained = 0;
		while (this.size > 0 && this.dueNanos[this.head] <= nowNanos) {
			this.head = (this.head + 1) % CAPACITY;
			this.size--;
			drained++;
		}
		return drained;
	}

	public void clear() {
		this.head = 0;
		this.size = 0;
	}

	public int pending() {
		return this.size;
	}
}
