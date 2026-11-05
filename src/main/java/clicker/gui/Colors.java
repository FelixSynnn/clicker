package clicker.gui;

/** Small ARGB helpers. */
public final class Colors {
	private Colors() {
	}

	public static int alpha(int argb) {
		return argb >>> 24;
	}

	public static int withAlpha(int argb, int alpha) {
		return (Math.max(0, Math.min(255, alpha)) << 24) | (argb & 0x00FFFFFF);
	}

	/** Multiplies the colour's alpha by {@code factor}. */
	public static int fade(int argb, float factor) {
		if (factor >= 1f) {
			return argb;
		}
		if (factor <= 0f) {
			return argb & 0x00FFFFFF;
		}
		return withAlpha(argb, Math.round(alpha(argb) * factor));
	}

	public static int lerp(int from, int to, float t) {
		if (t <= 0f) {
			return from;
		}
		if (t >= 1f) {
			return to;
		}
		int a = lerpChannel(from >>> 24, to >>> 24, t);
		int r = lerpChannel((from >> 16) & 0xFF, (to >> 16) & 0xFF, t);
		int g = lerpChannel((from >> 8) & 0xFF, (to >> 8) & 0xFF, t);
		int b = lerpChannel(from & 0xFF, to & 0xFF, t);
		return (a << 24) | (r << 16) | (g << 8) | b;
	}

	private static int lerpChannel(int from, int to, float t) {
		return Math.round(from + (to - from) * t);
	}

	/** Blends {@code overlay} on top of an opaque {@code base} colour (result is opaque). */
	public static int over(int base, int overlay) {
		float a = alpha(overlay) / 255f;
		return lerp(base | 0xFF000000, overlay | 0xFF000000, a);
	}
}
