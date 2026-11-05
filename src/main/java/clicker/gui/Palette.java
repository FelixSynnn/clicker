package clicker.gui;

/**
 * The colours of one theme (all ARGB).
 *
 * <p>Palettes are immutable; {@link #lerp} blends two of them, which is how theme switches animate.
 */
public final class Palette {
	private static final int BACKDROP_TOP = 0;
	private static final int BACKDROP_BOTTOM = 1;
	private static final int WINDOW = 2;
	private static final int WINDOW_EDGE = 3;
	private static final int SIDEBAR = 4;
	private static final int CARD = 5;
	private static final int CARD_EDGE = 6;
	private static final int HOVER = 7;
	private static final int TEXT = 8;
	private static final int TEXT_SECONDARY = 9;
	private static final int TEXT_MUTED = 10;
	private static final int ACCENT = 11;
	private static final int ACCENT_STRONG = 12;
	private static final int ACCENT_SOFT = 13;
	private static final int ON_ACCENT = 14;
	private static final int TRACK = 15;
	private static final int KNOB = 16;
	private static final int NAV_ACTIVE = 17;
	private static final int SHADOW = 18;
	private static final int SUCCESS = 19;
	private static final int WARNING = 20;
	private static final int DANGER = 21;
	private static final int PETAL = 22;
	private static final int COUNT = 23;

	private final int[] colors;

	private Palette(int[] colors) {
		this.colors = colors;
	}

	/** Tint drawn over the blurred world, top of the screen. */
	public int backdropTop() {
		return this.colors[BACKDROP_TOP];
	}

	/** Tint drawn over the blurred world, bottom of the screen. */
	public int backdropBottom() {
		return this.colors[BACKDROP_BOTTOM];
	}

	public int window() {
		return this.colors[WINDOW];
	}

	public int windowEdge() {
		return this.colors[WINDOW_EDGE];
	}

	public int sidebar() {
		return this.colors[SIDEBAR];
	}

	public int card() {
		return this.colors[CARD];
	}

	public int cardEdge() {
		return this.colors[CARD_EDGE];
	}

	/** Translucent overlay for hovered elements. */
	public int hover() {
		return this.colors[HOVER];
	}

	public int text() {
		return this.colors[TEXT];
	}

	public int textSecondary() {
		return this.colors[TEXT_SECONDARY];
	}

	public int textMuted() {
		return this.colors[TEXT_MUTED];
	}

	public int accent() {
		return this.colors[ACCENT];
	}

	public int accentStrong() {
		return this.colors[ACCENT_STRONG];
	}

	public int accentSoft() {
		return this.colors[ACCENT_SOFT];
	}

	/** Text/icon colour drawn on top of {@link #accent()}. */
	public int onAccent() {
		return this.colors[ON_ACCENT];
	}

	/** Inactive track for toggles and sliders. */
	public int track() {
		return this.colors[TRACK];
	}

	public int knob() {
		return this.colors[KNOB];
	}

	public int navActive() {
		return this.colors[NAV_ACTIVE];
	}

	public int shadow() {
		return this.colors[SHADOW];
	}

	public int success() {
		return this.colors[SUCCESS];
	}

	public int warning() {
		return this.colors[WARNING];
	}

	public int danger() {
		return this.colors[DANGER];
	}

	public int petal() {
		return this.colors[PETAL];
	}

	public static Palette lerp(Palette from, Palette to, float t) {
		if (t <= 0f) {
			return from;
		}
		if (t >= 1f) {
			return to;
		}
		int[] mixed = new int[COUNT];
		for (int i = 0; i < COUNT; i++) {
			mixed[i] = Colors.lerp(from.colors[i], to.colors[i], t);
		}
		return new Palette(mixed);
	}

	public static Builder builder() {
		return new Builder();
	}

	public static final class Builder {
		private final int[] colors = new int[COUNT];
		private final boolean[] set = new boolean[COUNT];

		private Builder put(int index, int argb) {
			this.colors[index] = argb;
			this.set[index] = true;
			return this;
		}

		public Builder backdrop(int top, int bottom) {
			return this.put(BACKDROP_TOP, top).put(BACKDROP_BOTTOM, bottom);
		}

		public Builder window(int fill, int edge) {
			return this.put(WINDOW, fill).put(WINDOW_EDGE, edge);
		}

		public Builder sidebar(int fill) {
			return this.put(SIDEBAR, fill);
		}

		public Builder card(int fill, int edge) {
			return this.put(CARD, fill).put(CARD_EDGE, edge);
		}

		public Builder hover(int argb) {
			return this.put(HOVER, argb);
		}

		public Builder text(int primary, int secondary, int muted) {
			return this.put(TEXT, primary).put(TEXT_SECONDARY, secondary).put(TEXT_MUTED, muted);
		}

		public Builder accent(int accent, int strong, int soft, int onAccent) {
			return this.put(ACCENT, accent).put(ACCENT_STRONG, strong).put(ACCENT_SOFT, soft).put(ON_ACCENT, onAccent);
		}

		public Builder controls(int track, int knob, int navActive) {
			return this.put(TRACK, track).put(KNOB, knob).put(NAV_ACTIVE, navActive);
		}

		public Builder shadow(int argb) {
			return this.put(SHADOW, argb);
		}

		public Builder status(int success, int warning, int danger) {
			return this.put(SUCCESS, success).put(WARNING, warning).put(DANGER, danger);
		}

		public Builder petal(int argb) {
			return this.put(PETAL, argb);
		}

		public Palette build() {
			for (int i = 0; i < COUNT; i++) {
				if (!this.set[i]) {
					throw new IllegalStateException("Palette colour " + i + " was not set");
				}
			}
			return new Palette(this.colors.clone());
		}
	}
}
