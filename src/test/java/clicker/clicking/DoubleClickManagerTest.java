package clicker.clicking;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DoubleClickManagerTest {
	private static final long MS = 1_000_000L;

	@Test
	void secondClickFiresAfterTheDelay() {
		DoubleClickManager manager = new DoubleClickManager();
		manager.schedule(50 * MS);
		assertEquals(0, manager.drainDue(49 * MS));
		assertEquals(1, manager.drainDue(50 * MS));
		assertEquals(0, manager.drainDue(500 * MS), "each second click fires exactly once");
	}

	@Test
	void pairsStayPairedAcrossFrames() {
		// 10 CPS with a 50 ms delay, simulated at 60 FPS for one second: 10 firsts and 10 seconds.
		ClickScheduler scheduler = new ClickScheduler();
		DoubleClickManager doubles = new DoubleClickManager();
		long[] times = new long[ClickScheduler.MAX_CLICKS_PER_FRAME];
		long interval = ClickScheduler.intervalNanos(10);
		scheduler.start(0, interval, false);
		int firsts = 0;
		int seconds = 0;
		for (long now = 0; now < 1_000 * MS; now += 1_000 * MS / 60) {
			int due = scheduler.poll(now, interval, times);
			for (int i = 0; i < due; i++) {
				firsts++;
				doubles.schedule(times[i] + 50 * MS);
			}
			seconds += doubles.drainDue(now);
		}
		seconds += doubles.drainDue(1_100 * MS);
		assertEquals(10, firsts);
		assertEquals(10, seconds);
	}

	@Test
	void delayLongerThanIntervalStillPairsEveryClick() {
		ClickScheduler scheduler = new ClickScheduler();
		DoubleClickManager doubles = new DoubleClickManager();
		long[] times = new long[ClickScheduler.MAX_CLICKS_PER_FRAME];
		long interval = ClickScheduler.intervalNanos(20); // 50 ms
		scheduler.start(0, interval, false);
		int firsts = 0;
		int seconds = 0;
		for (long now = 0; now < 2_000 * MS; now += MS * 1000 / 144) {
			int due = scheduler.poll(now, interval, times);
			for (int i = 0; i < due; i++) {
				firsts++;
				doubles.schedule(times[i] + 300 * MS);
			}
			seconds += doubles.drainDue(now);
		}
		seconds += doubles.drainDue(3_000 * MS);
		assertEquals(firsts, seconds);
	}

	@Test
	void queueIsBoundedAndClearable() {
		DoubleClickManager manager = new DoubleClickManager();
		for (int i = 0; i < DoubleClickManager.CAPACITY; i++) {
			assertTrue(manager.schedule(i));
		}
		assertFalse(manager.schedule(1_000), "a full queue drops instead of growing");
		assertEquals(DoubleClickManager.CAPACITY, manager.pending());
		manager.clear();
		assertEquals(0, manager.pending());
		assertEquals(0, manager.drainDue(Long.MAX_VALUE));
	}

	@Test
	void drainingNeverSchedulesMore() {
		DoubleClickManager manager = new DoubleClickManager();
		manager.schedule(10);
		manager.schedule(20);
		assertEquals(2, manager.drainDue(100));
		assertEquals(0, manager.pending());
	}
}
