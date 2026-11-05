package clicker.gui.anim;

import clicker.ClickerClient;
import clicker.config.ConfigManager;

/**
 * Frame-rate independent smoothing. Values ease exponentially towards their target, so motion looks the same at
 * 30 or 300 FPS. When animations are disabled in the settings, values snap instantly.
 */
public final class Anim {
	private Anim() {
	}

	public static boolean enabled() {
		ConfigManager config = ClickerClient.config();
		return config == null || config.get().animations;
	}

	/**
	 * @param speed higher is faster; ~10 is relaxed, ~20 is snappy
	 */
	public static float approach(float current, float target, float dtSeconds, float speed) {
		if (!enabled()) {
			return target;
		}
		if (dtSeconds <= 0f) {
			return current;
		}
		float k = 1f - (float) Math.exp(-speed * dtSeconds);
		float value = current + (target - current) * k;
		if (Math.abs(target - value) < 0.0005f) {
			return target;
		}
		return value;
	}

	public static float lerp(float from, float to, float t) {
		return from + (to - from) * t;
	}
}
