package clicker.config;

import clicker.clicking.ActivationMode;
import clicker.clicking.ClickType;

/**
 * Every persistent Clicker setting, with its default value.
 *
 * <p>Keybinds are stored by Minecraft itself in {@code options.txt}, like every other control.
 */
public final class ClickerConfig {
	public static final int CONFIG_VERSION = 1;

	public static final int MIN_CPS = 1;
	public static final int MAX_CPS = 100;
	public static final int DEFAULT_CPS = 10;

	public static final int MIN_DOUBLE_CLICK_DELAY_MS = 1;
	public static final int MAX_DOUBLE_CLICK_DELAY_MS = 500;
	public static final int DEFAULT_DOUBLE_CLICK_DELAY_MS = 50;

	/** 0 means "match Minecraft's GUI scale"; 1-4 force that scale (reduced automatically if it would not fit). */
	public static final int MAX_GUI_SCALE = 4;

	public static final String DEFAULT_THEME = "sakura";
	public static final String DEFAULT_PAGE = "home";

	public final SideConfig left = new SideConfig(true);
	public final SideConfig right = new SideConfig(false);

	/** Master switch for double clicking; each side also has its own toggle and delay. */
	public boolean doubleClickEnabled = false;

	public boolean protectionEnabled = true;
	public boolean protectElytra = true;
	public boolean protectGoldenApple = true;
	public boolean protectEnchantedGoldenApple = true;
	public boolean protectChestplates = true;

	public String theme = DEFAULT_THEME;
	public int guiScale = 0;
	public boolean animations = true;
	public boolean sounds = true;
	public HudMode hud = HudMode.WHEN_ACTIVE;
	public String lastPage = DEFAULT_PAGE;

	/** When true, the on/off state survives restarts and disconnects. Off by default for safety. */
	public boolean rememberState = false;
	public boolean lastGlobalState = false;

	public SideConfig side(ClickType type) {
		return type == ClickType.LEFT ? this.left : this.right;
	}

	public boolean doubleClickActive(ClickType type) {
		return this.doubleClickEnabled && this.side(type).doubleClick;
	}

	/** Copies every value from {@code other} into this instance (used by reset, so references stay valid). */
	public void copyFrom(ClickerConfig other) {
		this.left.copyFrom(other.left);
		this.right.copyFrom(other.right);
		this.doubleClickEnabled = other.doubleClickEnabled;
		this.protectionEnabled = other.protectionEnabled;
		this.protectElytra = other.protectElytra;
		this.protectGoldenApple = other.protectGoldenApple;
		this.protectEnchantedGoldenApple = other.protectEnchantedGoldenApple;
		this.protectChestplates = other.protectChestplates;
		this.theme = other.theme;
		this.guiScale = other.guiScale;
		this.animations = other.animations;
		this.sounds = other.sounds;
		this.hud = other.hud;
		this.lastPage = other.lastPage;
		this.rememberState = other.rememberState;
		this.lastGlobalState = other.lastGlobalState;
	}

	/** Clamps every value into its valid range. */
	public void validate() {
		this.left.validate();
		this.right.validate();
		this.guiScale = clamp(this.guiScale, 0, MAX_GUI_SCALE);
		if (this.hud == null) {
			this.hud = HudMode.WHEN_ACTIVE;
		}
		if (this.theme == null || this.theme.isBlank()) {
			this.theme = DEFAULT_THEME;
		}
		if (this.lastPage == null || this.lastPage.isBlank()) {
			this.lastPage = DEFAULT_PAGE;
		}
	}

	public static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	/** Settings for one side (left or right click). */
	public static final class SideConfig {
		public boolean enabled;
		public int cps = DEFAULT_CPS;
		public ActivationMode mode = ActivationMode.HOLD;
		public boolean doubleClick = true;
		public int doubleClickDelayMs = DEFAULT_DOUBLE_CLICK_DELAY_MS;

		SideConfig(boolean enabled) {
			this.enabled = enabled;
		}

		void copyFrom(SideConfig other) {
			this.enabled = other.enabled;
			this.cps = other.cps;
			this.mode = other.mode;
			this.doubleClick = other.doubleClick;
			this.doubleClickDelayMs = other.doubleClickDelayMs;
		}

		void validate() {
			this.cps = clamp(this.cps, MIN_CPS, MAX_CPS);
			this.doubleClickDelayMs = clamp(this.doubleClickDelayMs, MIN_DOUBLE_CLICK_DELAY_MS, MAX_DOUBLE_CLICK_DELAY_MS);
			if (this.mode == null) {
				this.mode = ActivationMode.HOLD;
			}
		}
	}
}
