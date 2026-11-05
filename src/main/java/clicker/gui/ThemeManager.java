package clicker.gui;

import clicker.config.ConfigManager;
import clicker.gui.anim.Easing;

/** Tracks the selected theme and blends palettes when it changes. */
public final class ThemeManager {
	private static final long TRANSITION_NANOS = 380_000_000L;

	private final ConfigManager config;
	private ClickerTheme current;
	private Palette from;
	private long transitionStart = Long.MIN_VALUE;

	public ThemeManager(ConfigManager config) {
		this.config = config;
		if (!ClickerTheme.exists(config.get().theme)) {
			// Unknown theme id (renamed preset or hand-edited file): fall back to Sakura and persist that.
			config.get().theme = ClickerTheme.SAKURA.id();
			config.markDirty();
		}
		this.current = ClickerTheme.byId(config.get().theme);
		this.from = this.current.palette();
		// Follow config changes, including "reset settings".
		config.addListener(cfg -> {
			if (!this.current.id().equals(cfg.theme)) {
				this.switchTo(ClickerTheme.byId(cfg.theme));
			}
		});
	}

	public ClickerTheme current() {
		return this.current;
	}

	/** Selects and saves a theme; the change animates if animations are enabled. */
	public void select(ClickerTheme theme) {
		if (theme.id().equals(this.config.get().theme) && theme == this.current) {
			return;
		}
		this.config.get().theme = theme.id();
		this.switchTo(theme);
		this.config.markDirty();
	}

	private void switchTo(ClickerTheme theme) {
		if (theme == this.current) {
			return;
		}
		this.from = this.palette();
		this.current = theme;
		this.transitionStart = System.nanoTime();
	}

	/** The palette to draw with right now (mid-transition palettes are blended). */
	public Palette palette() {
		if (!this.config.get().animations || this.transitionStart == Long.MIN_VALUE) {
			return this.current.palette();
		}
		float t = (System.nanoTime() - this.transitionStart) / (float) TRANSITION_NANOS;
		if (t >= 1f) {
			this.transitionStart = Long.MIN_VALUE;
			return this.current.palette();
		}
		return Palette.lerp(this.from, this.current.palette(), Easing.inOutCubic(t));
	}
}
