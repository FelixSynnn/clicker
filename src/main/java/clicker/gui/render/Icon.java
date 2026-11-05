package clicker.gui.render;

/**
 * Clicker's icon set, defined as vector shapes on a 24x24 grid and rasterised at the exact pixel size needed.
 */
public enum Icon {
	HOME {
		@Override
		float alpha(float x, float y, float k) {
			float d = stroke(Sdf.segment(x, y, 4f, 11.2f, 12f, 4.6f));
			d = Math.min(d, stroke(Sdf.segment(x, y, 12f, 4.6f, 20f, 11.2f)));
			d = Math.min(d, stroke(Sdf.segment(x, y, 6.5f, 10f, 6.5f, 19.5f)));
			d = Math.min(d, stroke(Sdf.segment(x, y, 6.5f, 19.5f, 17.5f, 19.5f)));
			d = Math.min(d, stroke(Sdf.segment(x, y, 17.5f, 19.5f, 17.5f, 10f)));
			d = Math.min(d, Sdf.roundBox(x, y, 12f, 16.4f, 1.9f, 3.1f, 0.8f));
			return cover(d, k);
		}
	},
	CLICK {
		@Override
		float alpha(float x, float y, float k) {
			float d = Math.min(mouseOutline(x, y), mouseDividers(x, y));
			// Two small "click" sparks.
			d = Math.min(d, stroke(Sdf.segment(x, y, 19.8f, 3.2f, 21.2f, 1.8f), 1.6f));
			d = Math.min(d, stroke(Sdf.segment(x, y, 20.6f, 6.4f, 22.4f, 6.4f), 1.6f));
			return cover(d, k);
		}
	},
	MOUSE_LEFT {
		@Override
		float alpha(float x, float y, float k) {
			float fill = Math.max(mouseInside(x, y), Math.max(x - 11.1f, y - 10.3f));
			return cover(Math.min(Math.min(mouseOutline(x, y), mouseDividers(x, y)), fill), k);
		}
	},
	MOUSE_RIGHT {
		@Override
		float alpha(float x, float y, float k) {
			float fill = Math.max(mouseInside(x, y), Math.max(12.9f - x, y - 10.3f));
			return cover(Math.min(Math.min(mouseOutline(x, y), mouseDividers(x, y)), fill), k);
		}
	},
	DOUBLE {
		@Override
		float alpha(float x, float y, float k) {
			float core = Sdf.circle(x, y, 12f, 12f, 2.7f);
			float ring = stroke(Sdf.circle(x, y, 12f, 12f, 6.4f), 1.9f);
			float outer = stroke(Sdf.circle(x, y, 12f, 12f, 10f), 1.6f);
			return Math.max(cover(Math.min(core, ring), k), 0.45f * cover(outer, k));
		}
	},
	SHIELD {
		@Override
		float alpha(float x, float y, float k) {
			float d = stroke(Sdf.polygon(x, y, Shapes.SHIELD));
			d = Math.min(d, stroke(Sdf.segment(x, y, 8.7f, 12.1f, 11f, 14.4f)));
			d = Math.min(d, stroke(Sdf.segment(x, y, 11f, 14.4f, 15.5f, 9.7f)));
			return cover(d, k);
		}
	},
	PALETTE {
		@Override
		float alpha(float x, float y, float k) {
			float d = stroke(Sdf.circle(x, y, 12f, 12f, 8.6f));
			d = Math.min(d, Sdf.circle(x, y, 8.3f, 10.7f, 1.6f));
			d = Math.min(d, Sdf.circle(x, y, 11.5f, 7.7f, 1.6f));
			d = Math.min(d, Sdf.circle(x, y, 15.4f, 9.4f, 1.6f));
			d = Math.min(d, Sdf.circle(x, y, 14.4f, 15.2f, 2.1f));
			return cover(d, k);
		}
	},
	GEAR {
		@Override
		float alpha(float x, float y, float k) {
			float d = Sdf.circle(x, y, 12f, 12f, 6.3f);
			for (int i = 0; i < 8; i++) {
				double angle = i * Math.PI / 4.0;
				float cos = (float) Math.cos(angle);
				float sin = (float) Math.sin(angle);
				float dx = x - 12f;
				float dy = y - 12f;
				float rx = dx * cos + dy * sin + 12f;
				float ry = -dx * sin + dy * cos + 12f;
				d = Math.min(d, Sdf.roundBox(rx, ry, 12f, 4.3f, 1.7f, 2.2f, 0.6f));
			}
			d = Sdf.subtract(d, Sdf.circle(x, y, 12f, 12f, 2.7f));
			return cover(d, k);
		}
	},
	POWER {
		@Override
		float alpha(float x, float y, float k) {
			float d = arc(x, y, 12f, 13f, 7.4f, -90f, 40f, 2.1f);
			d = Math.min(d, stroke(Sdf.segment(x, y, 12f, 3.4f, 12f, 11.4f), 2.1f));
			return cover(d, k);
		}
	},
	CHECK {
		@Override
		float alpha(float x, float y, float k) {
			float d = stroke(Sdf.segment(x, y, 5.8f, 12.6f, 10f, 16.8f), 2.6f);
			d = Math.min(d, stroke(Sdf.segment(x, y, 10f, 16.8f, 18.4f, 7.8f), 2.6f));
			return cover(d, k);
		}
	},
	INFO {
		@Override
		float alpha(float x, float y, float k) {
			float d = stroke(Sdf.circle(x, y, 12f, 12f, 8.8f), 1.9f);
			d = Math.min(d, Sdf.circle(x, y, 12f, 7.9f, 1.35f));
			d = Math.min(d, stroke(Sdf.segment(x, y, 12f, 11.2f, 12f, 16.4f), 2.1f));
			return cover(d, k);
		}
	},
	KEYBOARD {
		@Override
		float alpha(float x, float y, float k) {
			float d = stroke(Sdf.roundBox(x, y, 12f, 12f, 9.2f, 6.2f, 2.2f), 1.8f);
			for (int i = 0; i < 4; i++) {
				float kx = 7.4f + i * 3.07f;
				d = Math.min(d, Sdf.roundBox(x, y, kx, 9.9f, 0.95f, 0.95f, 0.3f));
			}
			for (int i = 0; i < 3; i++) {
				float kx = 8.9f + i * 3.07f;
				d = Math.min(d, Sdf.roundBox(x, y, kx, 12.7f, 0.95f, 0.95f, 0.3f));
			}
			d = Math.min(d, Sdf.roundBox(x, y, 12f, 15.5f, 3.8f, 0.9f, 0.45f));
			return cover(d, k);
		}
	},
	CLOCK {
		@Override
		float alpha(float x, float y, float k) {
			float d = stroke(Sdf.circle(x, y, 12f, 12f, 8.8f), 1.9f);
			d = Math.min(d, stroke(Sdf.segment(x, y, 12f, 12f, 12f, 7.2f), 2f));
			d = Math.min(d, stroke(Sdf.segment(x, y, 12f, 12f, 15.4f, 14f), 2f));
			return cover(d, k);
		}
	},
	RESET {
		@Override
		float alpha(float x, float y, float k) {
			float d = arc(x, y, 12f, 12.6f, 7.4f, -55f, 32f, 2.1f);
			d = Math.min(d, Sdf.polygon(x, y, Shapes.ARROW_HEAD));
			return cover(d, k);
		}
	},
	SPARKLE {
		@Override
		float alpha(float x, float y, float k) {
			// A four-pointed star made of two thin diamonds, plus a small companion dot.
			float dx = Math.abs(x - 12f);
			float dy = Math.abs(y - 12f);
			float vertical = (dx / 2.4f + dy / 9f - 1f) * 2.2f;
			float horizontal = (dx / 9f + dy / 2.4f - 1f) * 2.2f;
			float small = Sdf.circle(x, y, 19.2f, 4.8f, 1.7f);
			return cover(Math.min(Math.min(vertical, horizontal), small), k);
		}
	};

	/** Polygon data shared by the icons above. */
	private static final class Shapes {
		static final float[] SHIELD = {
				12f, 3.2f, 19f, 5.8f, 19f, 11.5f, 17.8f, 15.4f, 15.2f, 18.6f,
				12f, 20.8f, 8.8f, 18.6f, 6.2f, 15.4f, 5f, 11.5f, 5f, 5.8f
		};
		static final float[] ARROW_HEAD = {16.9f, 2.2f, 18.9f, 8.6f, 12.6f, 7.4f};
	}

	/** Coverage (0..1) at design coordinates (x, y); {@code k} is pixels per design unit. */
	abstract float alpha(float x, float y, float k);

	private static float stroke(float distance) {
		return Sdf.stroke(distance, 2f);
	}

	private static float stroke(float distance, float width) {
		return Sdf.stroke(distance, width);
	}

	private static float cover(float designDistance, float k) {
		return Sdf.coverage(designDistance * k);
	}

	private static float mouseOutline(float x, float y) {
		return Sdf.stroke(Sdf.roundBox(x, y, 12f, 12.6f, 6.4f, 9.1f, 6.4f), 1.8f);
	}

	/** The button split: a vertical line in the top half and a horizontal line under the buttons. */
	private static float mouseDividers(float x, float y) {
		float vertical = Sdf.stroke(Sdf.segment(x, y, 12f, 3.6f, 12f, 11.2f), 1.6f);
		float horizontal = Sdf.stroke(Sdf.segment(x, y, 5.8f, 11.2f, 18.2f, 11.2f), 1.6f);
		return Math.min(vertical, horizontal);
	}

	/** The area just inside the mouse outline. */
	private static float mouseInside(float x, float y) {
		return Sdf.roundBox(x, y, 12f, 12.6f, 5.5f, 8.2f, 5.5f);
	}

	/** Distance to an arc with a gap centred on {@code gapAngleDeg} (screen angles, -90 is up). */
	private static float arc(float x, float y, float cx, float cy, float r, float gapAngleDeg, float gapHalfDeg, float width) {
		double angle = Math.toDegrees(Math.atan2(y - cy, x - cx));
		double diff = Math.abs(((angle - gapAngleDeg) % 360 + 540) % 360 - 180);
		if (diff > gapHalfDeg) {
			return Math.abs(Sdf.circle(x, y, cx, cy, r)) - width / 2f;
		}
		double a1 = Math.toRadians(gapAngleDeg - gapHalfDeg);
		double a2 = Math.toRadians(gapAngleDeg + gapHalfDeg);
		float d1 = (float) Math.hypot(x - (cx + r * Math.cos(a1)), y - (cy + r * Math.sin(a1)));
		float d2 = (float) Math.hypot(x - (cx + r * Math.cos(a2)), y - (cy + r * Math.sin(a2)));
		return Math.min(d1, d2) - width / 2f;
	}
}
