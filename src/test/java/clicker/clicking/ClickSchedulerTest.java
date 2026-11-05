package clicker.clicking;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClickSchedulerTest {
	private static final long MS = 1_000_000L;
	private static final long SECOND = 1_000_000_000L;

	private final long[] times = new long[ClickScheduler.MAX_CLICKS_PER_FRAME];

	/** Simulates frames at a fixed frame rate and counts released clicks. */
	private int simulate(ClickScheduler scheduler, int cps, int fps, long durationNanos, long start) {
		long interval = ClickScheduler.intervalNanos(cps);
		long frame = SECOND / fps;
		int clicks = 0;
		for (long now = start; now < start + durationNanos; now += frame) {
			clicks += scheduler.poll(now, interval, this.times);
		}
		return clicks;
	}

	@Test
	void intervalMatchesCps() {
		assertEquals(100 * MS, ClickScheduler.intervalNanos(10));
		assertEquals(10 * MS, ClickScheduler.intervalNanos(100));
		assertEquals(SECOND, ClickScheduler.intervalNanos(1));
		assertEquals(SECOND, ClickScheduler.intervalNanos(0), "CPS below 1 is treated as 1");
	}

	@Test
	void toggleModeClicksImmediately() {
		ClickScheduler scheduler = new ClickScheduler();
		scheduler.start(0, 100 * MS, false);
		assertEquals(1, scheduler.poll(0, 100 * MS, this.times));
		assertEquals(0, scheduler.poll(99 * MS, 100 * MS, this.times));
		assertEquals(1, scheduler.poll(100 * MS, 100 * MS, this.times));
	}

	@Test
	void holdModeWaitsOneIntervalBecauseThePressAlreadyClicked() {
		ClickScheduler scheduler = new ClickScheduler();
		scheduler.start(0, 100 * MS, true);
		assertEquals(0, scheduler.poll(0, 100 * MS, this.times));
		assertEquals(0, scheduler.poll(99 * MS, 100 * MS, this.times));
		assertEquals(1, scheduler.poll(100 * MS, 100 * MS, this.times));
	}

	@Test
	void exactRateAcrossFrameRates() {
		for (int cps : new int[]{1, 7, 10, 20, 33, 50, 100}) {
			for (int fps : new int[]{20, 30, 60, 144, 240}) {
				ClickScheduler scheduler = new ClickScheduler();
				scheduler.start(0, ClickScheduler.intervalNanos(cps), false);
				int clicks = this.simulate(scheduler, cps, fps, 3 * SECOND, 0);
				int expected = cps * 3;
				// Clicks are released on frames, so the count can trail by at most one frame's worth.
				int tolerance = (int) Math.ceil((double) cps / fps) + 1;
				assertTrue(Math.abs(clicks - expected) <= tolerance,
						"cps=" + cps + " fps=" + fps + " expected ~" + expected + " but got " + clicks);
			}
		}
	}

	@Test
	void scheduledTimesAreEvenlySpaced() {
		ClickScheduler scheduler = new ClickScheduler();
		long interval = ClickScheduler.intervalNanos(100);
		scheduler.start(0, interval, false);
		int due = scheduler.poll(45 * MS, interval, this.times);
		assertEquals(5, due);
		for (int i = 0; i < due; i++) {
			assertEquals(i * interval, this.times[i]);
		}
	}

	@Test
	void neverReleasesMoreThanTheFrameCap() {
		ClickScheduler scheduler = new ClickScheduler();
		long interval = ClickScheduler.intervalNanos(100);
		scheduler.start(0, interval, false);
		// 200 ms frame at 100 CPS would be 20 clicks; the cap holds it to 10 and resynchronises.
		int due = scheduler.poll(200 * MS, interval, this.times);
		assertEquals(ClickScheduler.MAX_CLICKS_PER_FRAME, due);
		assertEquals(0, scheduler.poll(200 * MS, interval, this.times), "no burst on the following frame");
	}

	@Test
	void lagSpikeDoesNotBurst() {
		ClickScheduler scheduler = new ClickScheduler();
		long interval = ClickScheduler.intervalNanos(20);
		scheduler.start(0, interval, false);
		assertEquals(1, scheduler.poll(0, interval, this.times));
		// The game froze for two seconds: exactly one click, then the normal rhythm resumes.
		assertEquals(1, scheduler.poll(2 * SECOND, interval, this.times));
		assertEquals(0, scheduler.poll(2 * SECOND + interval - 1, interval, this.times));
		assertEquals(1, scheduler.poll(2 * SECOND + interval, interval, this.times));
	}

	@Test
	void raisingCpsTakesEffectWithinOneNewInterval() {
		ClickScheduler scheduler = new ClickScheduler();
		scheduler.start(0, ClickScheduler.intervalNanos(1), false);
		assertEquals(1, scheduler.poll(0, ClickScheduler.intervalNanos(1), this.times));
		long fast = ClickScheduler.intervalNanos(100);
		assertEquals(0, scheduler.poll(MS, fast, this.times));
		assertEquals(1, scheduler.poll(MS + fast, fast, this.times));
	}

	@Test
	void stoppedSchedulerNeverClicks() {
		ClickScheduler scheduler = new ClickScheduler();
		scheduler.start(0, 10 * MS, false);
		scheduler.stop();
		assertEquals(0, scheduler.poll(SECOND, 10 * MS, this.times));
		assertTrue(!scheduler.isRunning());
	}
}
