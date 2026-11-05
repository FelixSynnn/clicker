package clicker.gui.anim;

/** Easing curves for timed transitions (input and output are 0..1). */
public final class Easing {
	private Easing() {
	}

	public static float clamp01(float t) {
		return t < 0f ? 0f : (t > 1f ? 1f : t);
	}

	public static float outCubic(float t) {
		float u = 1f - clamp01(t);
		return 1f - u * u * u;
	}

	public static float inCubic(float t) {
		float c = clamp01(t);
		return c * c * c;
	}

	public static float inOutCubic(float t) {
		float c = clamp01(t);
		return c < 0.5f ? 4f * c * c * c : 1f - (float) Math.pow(-2f * c + 2f, 3) / 2f;
	}

	public static float outQuint(float t) {
		float u = 1f - clamp01(t);
		return 1f - u * u * u * u * u;
	}

	/** Slight overshoot, used for small "pop" effects. */
	public static float outBack(float t) {
		float c = clamp01(t);
		float c1 = 1.70158f;
		float c3 = c1 + 1f;
		return 1f + c3 * (float) Math.pow(c - 1f, 3) + c1 * (float) Math.pow(c - 1f, 2);
	}

	public static float smoothstep(float t) {
		float c = clamp01(t);
		return c * c * (3f - 2f * c);
	}
}
