import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.MultipleGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Generates Clicker's bundled artwork (mod icon, GUI logo layers and the falling-petal sprite).
 *
 * <p>Run from the project root: {@code java tools/GenerateAssets.java src/main/resources/assets/clicker}
 *
 * <p>Everything is drawn from vector shapes, so the output is reproducible and resolution independent.
 * GUI textures are white so the mod can tint them with the active theme colours at runtime.
 */
public final class GenerateAssets {
	private GenerateAssets() {
	}

	public static void main(String[] args) throws IOException {
		Path root = Path.of(args.length > 0 ? args[0] : "src/main/resources/assets/clicker");
		Path gui = root.resolve("textures/gui");
		Files.createDirectories(gui);

		write(modIcon(256, 128), root.resolve("icon.png"));
		write(logoPetals(128), gui.resolve("logo_petals.png"));
		write(logoCenter(128), gui.resolve("logo_center.png"));
		write(petalSprite(64), gui.resolve("petal.png"));

		// Linear filtering keeps the downscaled logo and rotated petals smooth.
		String blur = "{\n  \"texture\": {\n    \"blur\": true,\n    \"clamp\": true\n  }\n}\n";
		for (String name : new String[]{"logo_petals.png", "logo_center.png", "petal.png"}) {
			Files.writeString(gui.resolve(name + ".mcmeta"), blur);
		}
		System.out.println("Assets written to " + root.toAbsolutePath());
	}

	// ------------------------------------------------------------------ shapes

	/** One sakura petal in a unit box, base at (0.5, 0.96) and pointing up, with the classic notch. */
	private static Path2D petalPath() {
		Path2D.Double p = new Path2D.Double();
		p.moveTo(0.50, 0.96);
		p.curveTo(0.16, 0.80, 0.06, 0.40, 0.26, 0.14);
		p.curveTo(0.35, 0.03, 0.46, 0.05, 0.50, 0.17);
		p.curveTo(0.54, 0.05, 0.65, 0.03, 0.74, 0.14);
		p.curveTo(0.94, 0.40, 0.84, 0.80, 0.50, 0.96);
		p.closePath();
		return p;
	}

	/** A petal of the given length/width whose base sits at the origin and that points towards -Y. */
	private static Shape petalAtOrigin(double length, double width) {
		AffineTransform t = new AffineTransform();
		t.scale(width, length);
		t.translate(-0.5, -0.96);
		return t.createTransformedShape(petalPath());
	}

	private static Shape blossomPetal(double cx, double cy, double length, double width, double angleDeg) {
		AffineTransform t = new AffineTransform();
		t.translate(cx, cy);
		t.rotate(Math.toRadians(angleDeg));
		return t.createTransformedShape(petalAtOrigin(length, width));
	}

	// ------------------------------------------------------------------ images

	private static BufferedImage modIcon(int work, int out) {
		BufferedImage img = canvas(work);
		Graphics2D g = graphics(img);
		double s = work;

		// Soft rounded tile with a gentle vertical gradient.
		RoundRectangle2D tile = new RoundRectangle2D.Double(s * 0.03, s * 0.03, s * 0.94, s * 0.94, s * 0.30, s * 0.30);
		g.setPaint(new GradientPaint(0, (float) (s * 0.03), new Color(0xFFF1F6), 0, (float) (s * 0.97), new Color(0xF8B4CB)));
		g.fill(tile);

		// Click ripples behind the blossom.
		g.setStroke(new BasicStroke((float) (s * 0.018)));
		for (int i = 0; i < 3; i++) {
			double r = s * (0.30 + i * 0.075);
			g.setColor(new Color(255, 255, 255, 120 - i * 35));
			g.draw(new Ellipse2D.Double(s / 2 - r, s / 2 - r, r * 2, r * 2));
		}

		drawColourBlossom(g, s / 2, s * 0.505, s * 0.36);

		// A subtle top highlight on the tile.
		g.setClip(tile);
		g.setPaint(new GradientPaint(0, 0, new Color(255, 255, 255, 70), 0, (float) (s * 0.45), new Color(255, 255, 255, 0)));
		g.fill(new Ellipse2D.Double(-s * 0.2, -s * 0.55, s * 1.4, s * 0.95));
		g.setClip(null);
		g.dispose();
		return downscale(img, out);
	}

	private static void drawColourBlossom(Graphics2D g, double cx, double cy, double length) {
		double width = length * 0.78;
		RadialGradientPaint petalPaint = new RadialGradientPaint(
				new Point2D.Double(cx, cy), (float) length,
				new float[]{0f, 0.28f, 0.62f, 1f},
				new Color[]{new Color(0xE2507F), new Color(0xF48DB0), new Color(0xFFD3E2), new Color(0xFFF6F9)},
				MultipleGradientPaint.CycleMethod.NO_CYCLE);
		for (int i = 0; i < 5; i++) {
			Shape petal = blossomPetal(cx, cy, length, width, i * 72.0);
			g.setPaint(petalPaint);
			g.fill(petal);
			g.setStroke(new BasicStroke((float) (length * 0.025), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.setColor(new Color(214, 84, 128, 110));
			g.draw(petal);
		}
		drawStamens(g, cx, cy, length, new Color(0xC2306A), new Color(0xFFD36E), new Color(0xB8285E));
	}

	private static void drawStamens(Graphics2D g, double cx, double cy, double length, Color line, Color tip, Color core) {
		g.setStroke(new BasicStroke((float) (length * 0.035), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		for (int i = 0; i < 5; i++) {
			double a = Math.toRadians(i * 72.0 + 36.0 - 90.0);
			double r = length * 0.36;
			double x = cx + Math.cos(a) * r;
			double y = cy + Math.sin(a) * r;
			g.setColor(line);
			g.draw(new Line2D.Double(cx, cy, x, y));
			double d = length * 0.085;
			g.setColor(tip);
			g.fill(new Ellipse2D.Double(x - d / 2, y - d / 2, d, d));
		}
		double c = length * 0.2;
		g.setColor(core);
		g.fill(new Ellipse2D.Double(cx - c / 2, cy - c / 2, c, c));
	}

	/** White petals with soft alpha shading; tinted by the theme accent at runtime. */
	private static BufferedImage logoPetals(int size) {
		int work = size * 4;
		BufferedImage img = canvas(work);
		Graphics2D g = graphics(img);
		double cx = work / 2.0, cy = work * 0.51, length = work * 0.47;
		RadialGradientPaint paint = new RadialGradientPaint(
				new Point2D.Double(cx, cy), (float) length,
				new float[]{0f, 0.45f, 1f},
				new Color[]{new Color(255, 255, 255, 255), new Color(255, 255, 255, 235), new Color(255, 255, 255, 190)},
				MultipleGradientPaint.CycleMethod.NO_CYCLE);
		for (int i = 0; i < 5; i++) {
			g.setPaint(paint);
			g.fill(blossomPetal(cx, cy, length, length * 0.78, i * 72.0));
		}
		g.dispose();
		return downscale(img, size);
	}

	/** Centre and stamens of the logo, drawn on top of the petals in a deeper accent tone. */
	private static BufferedImage logoCenter(int size) {
		int work = size * 4;
		BufferedImage img = canvas(work);
		Graphics2D g = graphics(img);
		double cx = work / 2.0, cy = work * 0.51, length = work * 0.47;
		Color white = Color.WHITE;
		drawStamens(g, cx, cy, length, white, white, white);
		g.dispose();
		return downscale(img, size);
	}

	private static BufferedImage petalSprite(int size) {
		int work = size * 4;
		BufferedImage img = canvas(work);
		Graphics2D g = graphics(img);
		AffineTransform t = new AffineTransform();
		t.translate(work * 0.5, work * 0.97);
		Shape petal = t.createTransformedShape(petalAtOrigin(work * 0.94, work * 0.74));
		g.setPaint(new GradientPaint(0, work, new Color(255, 255, 255, 255), 0, 0, new Color(255, 255, 255, 200)));
		g.fill(petal);
		g.dispose();
		return downscale(img, size);
	}

	// ------------------------------------------------------------------ helpers

	private static BufferedImage canvas(int size) {
		return new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
	}

	private static Graphics2D graphics(BufferedImage img) {
		Graphics2D g = img.createGraphics();
		g.setComposite(AlphaComposite.SrcOver);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
		return g;
	}

	/** Box-filter downscale in premultiplied space so edges stay clean. */
	private static BufferedImage downscale(BufferedImage src, int size) {
		int factor = src.getWidth() / size;
		BufferedImage out = canvas(size);
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				double a = 0, r = 0, gg = 0, b = 0;
				for (int dy = 0; dy < factor; dy++) {
					for (int dx = 0; dx < factor; dx++) {
						int argb = src.getRGB(x * factor + dx, y * factor + dy);
						double pa = ((argb >>> 24) & 0xFF) / 255.0;
						a += pa;
						r += pa * ((argb >>> 16) & 0xFF);
						gg += pa * ((argb >>> 8) & 0xFF);
						b += pa * (argb & 0xFF);
					}
				}
				int n = factor * factor;
				int outA = (int) Math.round(a / n * 255.0);
				if (outA == 0) {
					out.setRGB(x, y, 0);
					continue;
				}
				int outR = clamp(r / a), outG = clamp(gg / a), outB = clamp(b / a);
				out.setRGB(x, y, (outA << 24) | (outR << 16) | (outG << 8) | outB);
			}
		}
		return out;
	}

	private static int clamp(double v) {
		return (int) Math.max(0, Math.min(255, Math.round(v)));
	}

	private static void write(BufferedImage img, Path path) throws IOException {
		Files.createDirectories(path.getParent());
		ImageIO.write(img, "png", path.toFile());
		System.out.println("  wrote " + path + " (" + img.getWidth() + "x" + img.getHeight() + ")");
	}
}
