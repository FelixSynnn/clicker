package clicker.clicking;

import java.util.Locale;

/**
 * How a clicker side decides when to click.
 *
 * <ul>
 *     <li>{@link #TOGGLE}: clicks continuously while Clicker is switched on (the global toggle key, G by default).</li>
 *     <li>{@link #HOLD}: clicks only while the physical attack/use button is held. Holding the button keeps
 *     generating clicks at the chosen CPS, so you never have to spam the button yourself.</li>
 * </ul>
 */
public enum ActivationMode {
	TOGGLE("toggle"),
	HOLD("hold");

	private final String key;

	ActivationMode(String key) {
		this.key = key;
	}

	public String key() {
		return this.key;
	}

	public String translationKey() {
		return "clicker.mode." + this.key;
	}

	public static ActivationMode fromKey(String key, ActivationMode fallback) {
		if (key != null) {
			String normalized = key.trim().toLowerCase(Locale.ROOT);
			for (ActivationMode mode : values()) {
				if (mode.key.equals(normalized)) {
					return mode;
				}
			}
		}
		return fallback;
	}
}
