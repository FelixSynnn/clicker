package clicker.gui.render;

/**
 * Signed distance functions used to rasterise anti-aliased shapes and icons.
 * Negative values are inside the shape.
 */
final class Sdf {
	private Sdf() {
	}

	static float circle(float px, float py, float cx, float cy, float r) {
		return (float) Math.hypot(px - cx, py - cy) - r;
	}

	static float segment(float px, float py, float ax, float ay, float bx, float by) {
		float pax = px - ax;
		float pay = py - ay;
		float bax = bx - ax;
		float bay = by - ay;
		float lengthSq = bax * bax + bay * bay;
		float h = lengthSq == 0f ? 0f : clamp((pax * bax + pay * bay) / lengthSq, 0f, 1f);
		return (float) Math.hypot(pax - bax * h, pay - bay * h);
	}

	static float roundBox(float px, float py, float cx, float cy, float hx, float hy, float r) {
		float qx = Math.abs(px - cx) - hx + r;
		float qy = Math.abs(py - cy) - hy + r;
		float outside = (float) Math.hypot(Math.max(qx, 0f), Math.max(qy, 0f));
		float inside = Math.min(Math.max(qx, qy), 0f);
		return outside + inside - r;
	}

	/** Exact signed distance to a closed polygon given as x0,y0,x1,y1,... */
	static float polygon(float px, float py, float[] v) {
		int n = v.length / 2;
		float dx0 = px - v[0];
		float dy0 = py - v[1];
		float d = dx0 * dx0 + dy0 * dy0;
		float sign = 1f;
		for (int i = 0, j = n - 1; i < n; j = i, i++) {
			float vix = v[i * 2];
			float viy = v[i * 2 + 1];
			float vjx = v[j * 2];
			float vjy = v[j * 2 + 1];
			float ex = vjx - vix;
			float ey = vjy - viy;
			float wx = px - vix;
			float wy = py - viy;
			float t = clamp((wx * ex + wy * ey) / (ex * ex + ey * ey), 0f, 1f);
			float bx = wx - ex * t;
			float by = wy - ey * t;
			d = Math.min(d, bx * bx + by * by);
			boolean c1 = py >= viy;
			boolean c2 = py < vjy;
			boolean c3 = ex * wy > ey * wx;
			if ((c1 && c2 && c3) || (!c1 && !c2 && !c3)) {
				sign = -sign;
			}
		}
		return sign * (float) Math.sqrt(d);
	}

	/** Turns a filled shape into an outline of the given width. */
	static float stroke(float d, float width) {
		return Math.abs(d) - width / 2f;
	}

	static float union(float a, float b) {
		return Math.min(a, b);
	}

	static float subtract(float a, float b) {
		return Math.max(a, -b);
	}

	static float intersect(float a, float b) {
		return Math.max(a, b);
	}

	/** Pixel coverage for a distance measured in pixels. */
	static float coverage(float distancePx) {
		return clamp(0.5f - distancePx, 0f, 1f);
	}

	static float clamp(float v, float min, float max) {
		return v < min ? min : (v > max ? max : v);
	}
}
