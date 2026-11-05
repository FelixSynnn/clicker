package clicker.gui.render;

import clicker.Clicker;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;

/**
 * Lazily generates and caches the white, alpha-only textures used to draw rounded shapes, outlines, soft shadows
 * and icons. Every texture is generated at the exact pixel size it is drawn at, so edges are perfectly
 * anti-aliased at any GUI scale. Textures are tinted with the theme colour when drawn.
 */
final class ShapeTextures {
	private static final int MAX_RADIUS = 256;

	private final Map<Integer, Identifier> circles = new HashMap<>();
	private final Map<Long, Identifier> rings = new HashMap<>();
	private final Map<Long, Shadow> shadows = new HashMap<>();
	private final Map<Long, Identifier> icons = new HashMap<>();
	private int generated;

	/** A {@code 2r x 2r} anti-aliased disc. */
	Identifier circle(int radius) {
		int r = Math.max(1, Math.min(MAX_RADIUS, radius));
		return this.circles.computeIfAbsent(r, key -> {
			int size = r * 2;
			NativeImage image = new NativeImage(size, size, false);
			for (int y = 0; y < size; y++) {
				for (int x = 0; x < size; x++) {
					float d = Sdf.circle(x + 0.5f, y + 0.5f, r, r, r);
					image.setPixel(x, y, white(Sdf.coverage(d)));
				}
			}
			return this.register("circle_" + r, image);
		});
	}

	/** A {@code 2r x 2r} anti-aliased ring of the given thickness. */
	Identifier ring(int radius, int thickness) {
		int r = Math.max(1, Math.min(MAX_RADIUS, radius));
		int t = Math.max(1, Math.min(r, thickness));
		long key = ((long) r << 32) | t;
		return this.rings.computeIfAbsent(key, k -> {
			int size = r * 2;
			NativeImage image = new NativeImage(size, size, false);
			for (int y = 0; y < size; y++) {
				for (int x = 0; x < size; x++) {
					float d = Sdf.stroke(Sdf.circle(x + 0.5f, y + 0.5f, r, r, r - t / 2f), t);
					image.setPixel(x, y, white(Sdf.coverage(d)));
				}
			}
			return this.register("ring_" + r + "_" + t, image);
		});
	}

	/**
	 * A nine-slice soft shadow template for a rounded rectangle with corner radius {@code radius} blurred by
	 * {@code blur} pixels. The template is {@code size x size} with {@code corner x corner} corner pieces and a
	 * single stretchable middle row/column.
	 */
	Shadow shadow(int radius, int blur) {
		int r = Math.max(0, Math.min(MAX_RADIUS, radius));
		int b = Math.max(1, Math.min(MAX_RADIUS, blur));
		long key = ((long) r << 32) | b;
		return this.shadows.computeIfAbsent(key, k -> {
			int corner = r + 2 * b;
			int size = corner * 2 + 1;
			float[] mask = new float[size * size];
			float half = (size - 2f * b) / 2f;
			float center = size / 2f;
			for (int y = 0; y < size; y++) {
				for (int x = 0; x < size; x++) {
					float d = Sdf.roundBox(x + 0.5f, y + 0.5f, center, center, half, half, r);
					mask[y * size + x] = Sdf.coverage(d);
				}
			}
			float[] blurred = gaussianBlur(mask, size, b / 2f);
			NativeImage image = new NativeImage(size, size, false);
			for (int y = 0; y < size; y++) {
				for (int x = 0; x < size; x++) {
					image.setPixel(x, y, white(blurred[y * size + x]));
				}
			}
			Identifier id = this.register("shadow_" + r + "_" + b, image);
			return new Shadow(id, size, corner);
		});
	}

	/** An icon rasterised at {@code sizePx x sizePx}. */
	Identifier icon(Icon icon, int sizePx) {
		int size = Math.max(4, Math.min(256, sizePx));
		long key = ((long) icon.ordinal() << 32) | size;
		return this.icons.computeIfAbsent(key, k -> {
			NativeImage image = new NativeImage(size, size, false);
			float scale = size / 24f;
			for (int y = 0; y < size; y++) {
				for (int x = 0; x < size; x++) {
					float a = icon.alpha((x + 0.5f) / scale, (y + 0.5f) / scale, scale);
					image.setPixel(x, y, white(a));
				}
			}
			return this.register("icon_" + icon.name().toLowerCase(java.util.Locale.ROOT) + "_" + size, image);
		});
	}

	int generatedCount() {
		return this.generated;
	}

	private Identifier register(String name, NativeImage image) {
		Identifier id = Clicker.id("generated/" + name);
		Minecraft.getInstance().getTextureManager().register(id, new ClickerTexture("clicker:" + name, image, false));
		this.generated++;
		return id;
	}

	private static int white(float alpha) {
		int a = Math.round(Sdf.clamp(alpha, 0f, 1f) * 255f);
		return (a << 24) | 0x00FFFFFF;
	}

	/**
	 * Approximates a Gaussian blur with three box blurs (running sums), so even large shadow templates are generated
	 * in well under a millisecond instead of causing a frame hitch.
	 */
	private static float[] gaussianBlur(float[] src, int size, float sigma) {
		// Three passes of a box of width w have variance (w^2 - 1) / 4.
		int radius = Math.max(1, Math.round(((float) Math.sqrt(4f * sigma * sigma + 1f) - 1f) / 2f));
		float[] a = src.clone();
		float[] b = new float[src.length];
		for (int pass = 0; pass < 3; pass++) {
			boxBlurHorizontal(a, b, size, radius);
			boxBlurVertical(b, a, size, radius);
		}
		return a;
	}

	private static void boxBlurHorizontal(float[] src, float[] dst, int size, int radius) {
		float scale = 1f / (radius * 2 + 1);
		for (int y = 0; y < size; y++) {
			int row = y * size;
			float sum = 0f;
			for (int i = -radius; i <= radius; i++) {
				sum += src[row + Math.max(0, Math.min(size - 1, i))];
			}
			for (int x = 0; x < size; x++) {
				dst[row + x] = sum * scale;
				int add = Math.min(size - 1, x + radius + 1);
				int remove = Math.max(0, x - radius);
				sum += src[row + add] - src[row + remove];
			}
		}
	}

	private static void boxBlurVertical(float[] src, float[] dst, int size, int radius) {
		float scale = 1f / (radius * 2 + 1);
		for (int x = 0; x < size; x++) {
			float sum = 0f;
			for (int i = -radius; i <= radius; i++) {
				sum += src[Math.max(0, Math.min(size - 1, i)) * size + x];
			}
			for (int y = 0; y < size; y++) {
				dst[y * size + x] = sum * scale;
				int add = Math.min(size - 1, y + radius + 1);
				int remove = Math.max(0, y - radius);
				sum += src[add * size + x] - src[remove * size + x];
			}
		}
	}

	record Shadow(Identifier texture, int size, int corner) {
	}
}
