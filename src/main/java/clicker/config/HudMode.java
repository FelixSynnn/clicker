package clicker.config;

import java.util.Locale;

/** When the small in-game status pill is shown. */
public enum HudMode {
	OFF("off"),
	WHEN_ACTIVE("when_active"),
	ALWAYS("always");

	private final String key;

	HudMode(String key) {
		this.key = key;
	}

	public String key() {
		return this.key;
	}

	public String translationKey() {
		return "clicker.hud." + this.key;
	}

	public static HudMode fromKey(String key, HudMode fallback) {
		if (key != null) {
			String normalized = key.trim().toLowerCase(Locale.ROOT);
			for (HudMode mode : values()) {
				if (mode.key.equals(normalized)) {
					return mode;
				}
			}
		}
		return fallback;
	}
}
