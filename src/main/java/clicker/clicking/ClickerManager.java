package clicker.clicking;

import clicker.config.ClickerConfig;
import clicker.config.ConfigManager;
import clicker.mixin.MinecraftInvoker;
import clicker.protection.ItemProtection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.jspecify.annotations.Nullable;

/**
 * Owns the clicker state and generates clicks.
 *
 * <p>Every frame (see {@code MinecraftMixin}) each side decides whether it should be clicking:
 * <ol>
 *     <li>Clicker must be switched on (global toggle) and the side enabled.</li>
 *     <li>The player must be in a world with no screen open, not paused, alive and not spectating.</li>
 *     <li>Toggle mode: always. Hold mode: only while the physical attack/use button is held.</li>
 *     <li>Right side only: no protected item in either hand.</li>
 * </ol>
 * When any condition fails the side stops immediately and drops pending double clicks.
 *
 * <p>Physical input is only ever <em>read</em> (via the vanilla key mappings); generated clicks call vanilla's
 * attack/use routines directly, so they cannot be mistaken for physical presses.
 */
public final class ClickerManager {
	private final ConfigManager config;
	private final ItemProtection protection;
	private final Side left = new Side(ClickType.LEFT);
	private final Side right = new Side(ClickType.RIGHT);
	private final long[] scheduledTimes = new long[ClickScheduler.MAX_CLICKS_PER_FRAME];

	private boolean globalEnabled;
	private boolean inFrame;
	private ItemProtection.@Nullable Match protectionMatch;

	public ClickerManager(ConfigManager config, ItemProtection protection) {
		this.config = config;
		this.protection = protection;
		ClickerConfig cfg = config.get();
		this.globalEnabled = cfg.rememberState && cfg.lastGlobalState;
	}

	// ------------------------------------------------------------------ state

	public boolean isGlobalEnabled() {
		return this.globalEnabled;
	}

	public void setGlobalEnabled(boolean enabled) {
		if (this.globalEnabled == enabled) {
			return;
		}
		this.globalEnabled = enabled;
		if (!enabled) {
			this.stopAll();
		}
		ClickerConfig cfg = this.config.get();
		if (cfg.rememberState && cfg.lastGlobalState != enabled) {
			cfg.lastGlobalState = enabled;
			this.config.markDirty();
		}
	}

	public boolean toggleGlobal() {
		this.setGlobalEnabled(!this.globalEnabled);
		return this.globalEnabled;
	}

	public boolean toggleSide(ClickType type) {
		ClickerConfig.SideConfig side = this.config.get().side(type);
		side.enabled = !side.enabled;
		this.config.markDirty();
		if (!side.enabled) {
			this.side(type).stop();
		}
		return side.enabled;
	}

	public Side side(ClickType type) {
		return type == ClickType.LEFT ? this.left : this.right;
	}

	/** What currently blocks right clicking (refreshed every frame while in a world), or null. */
	public ItemProtection.@Nullable Match protectionMatch() {
		return this.protectionMatch;
	}

	public ItemProtection protection() {
		return this.protection;
	}

	/** Leaving a world: stop everything, and switch off unless the user chose to remember the state. */
	public void onDisconnect() {
		this.stopAll();
		this.protectionMatch = null;
		if (!this.config.get().rememberState) {
			this.globalEnabled = false;
		}
	}

	public void stopAll() {
		this.left.stop();
		this.right.stop();
	}

	// ------------------------------------------------------------------ frame loop

	public void onFrame(Minecraft minecraft) {
		if (this.inFrame) {
			return; // never re-enter, whatever a click triggers
		}
		this.inFrame = true;
		try {
			long now = System.nanoTime();
			ClickerConfig cfg = this.config.get();
			boolean worldReady = isWorldReady(minecraft);
			this.protectionMatch = minecraft.player != null ? this.protection.check(minecraft.player) : null;
			this.updateSide(minecraft, this.left, cfg, worldReady, now);
			this.updateSide(minecraft, this.right, cfg, worldReady, now);
		} finally {
			this.inFrame = false;
		}
	}

	private static boolean isWorldReady(Minecraft minecraft) {
		LocalPlayer player = minecraft.player;
		return player != null
				&& minecraft.level != null
				&& minecraft.gameMode != null
				&& minecraft.screen == null
				&& minecraft.getOverlay() == null
				&& !minecraft.isPaused()
				&& minecraft.hitResult != null
				&& player.isAlive()
				&& !player.isSpectator();
	}

	private void updateSide(Minecraft minecraft, Side side, ClickerConfig cfg, boolean worldReady, long now) {
		ClickerConfig.SideConfig settings = cfg.side(side.type);

		SideStatus status;
		if (!this.globalEnabled) {
			status = SideStatus.OFF;
		} else if (!settings.enabled) {
			status = SideStatus.DISABLED;
		} else if (!worldReady || !isActivationHeld(minecraft, side.type, settings.mode)) {
			status = SideStatus.WAITING;
		} else if (side.type == ClickType.RIGHT && this.protectionMatch != null) {
			status = SideStatus.PROTECTED;
		} else {
			status = SideStatus.CLICKING;
		}
		side.status = status;

		if (status != SideStatus.CLICKING) {
			side.stop();
			return;
		}

		long interval = ClickScheduler.intervalNanos(settings.cps);
		if (!side.scheduler.isRunning()) {
			// In hold mode the physical press already clicked once, so wait one interval before the first extra click.
			side.scheduler.start(now, interval, settings.mode == ActivationMode.HOLD);
		}

		boolean doubleClick = cfg.doubleClickActive(side.type);
		long delayNanos = settings.doubleClickDelayMs * 1_000_000L;
		int due = side.scheduler.poll(now, interval, this.scheduledTimes);
		for (int i = 0; i < due; i++) {
			if (this.click(minecraft, side, now, false) && doubleClick) {
				side.doubleClicks.schedule(this.scheduledTimes[i] + delayNanos);
			}
		}

		if (!doubleClick) {
			side.doubleClicks.clear();
			return;
		}
		int seconds = side.doubleClicks.drainDue(now);
		for (int i = 0; i < seconds; i++) {
			// Second clicks are executed directly and never schedule anything themselves.
			this.click(minecraft, side, now, true);
		}
	}

	private static boolean isActivationHeld(Minecraft minecraft, ClickType type, ActivationMode mode) {
		if (mode == ActivationMode.TOGGLE) {
			return true;
		}
		// Reads the physical button state through the user's own attack/use bindings.
		return type == ClickType.LEFT ? minecraft.options.keyAttack.isDown() : minecraft.options.keyUse.isDown();
	}

	/** Performs one generated click. Returns false when the click was skipped. */
	private boolean click(Minecraft minecraft, Side side, long now, boolean secondary) {
		LocalPlayer player = minecraft.player;
		if (player == null || minecraft.gameMode == null || minecraft.hitResult == null) {
			return false;
		}
		// Vanilla ignores clicks while an item is being used (eating, drawing a bow, blocking...).
		if (player.isUsingItem()) {
			return false;
		}
		MinecraftInvoker invoker = (MinecraftInvoker) minecraft;
		if (side.type == ClickType.LEFT) {
			invoker.clicker$startAttack();
		} else {
			// Checked again per click: the held item can change between frames and between double-click halves.
			if (this.protection.blocksRightClick(player)) {
				side.status = SideStatus.PROTECTED;
				side.doubleClicks.clear();
				return false;
			}
			invoker.clicker$startUseItem();
		}
		side.stats.record(now, secondary);
		return true;
	}

	// ------------------------------------------------------------------ types

	public enum SideStatus {
		/** Clicker is switched off. */
		OFF,
		/** This side is disabled in the settings. */
		DISABLED,
		/** Armed, but not clicking right now (hold button not held, a screen is open, not in a world...). */
		WAITING,
		/** Right click paused because a protected item is held. */
		PROTECTED,
		/** Generating clicks. */
		CLICKING
	}

	/** Runtime state for one side. */
	public static final class Side {
		final ClickType type;
		final ClickScheduler scheduler = new ClickScheduler();
		final DoubleClickManager doubleClicks = new DoubleClickManager();
		final ClickStats stats = new ClickStats();
		SideStatus status = SideStatus.OFF;

		Side(ClickType type) {
			this.type = type;
		}

		void stop() {
			this.scheduler.stop();
			this.doubleClicks.clear();
		}

		public ClickType type() {
			return this.type;
		}

		public SideStatus status() {
			return this.status;
		}

		public ClickStats stats() {
			return this.stats;
		}

		public int pendingDoubleClicks() {
			return this.doubleClicks.pending();
		}

		public boolean isClicking() {
			return this.status == SideStatus.CLICKING;
		}
	}
}
