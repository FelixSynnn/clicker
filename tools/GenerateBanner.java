import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
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
import java.util.Arrays;

/**
 * Generates the README artwork: {@code docs/banner.png} (1280x400), {@code docs/social-preview.png} (1280x640)
 * and {@code docs/themes.png} (a 3x2 collage of the six theme screenshots).
 *
 * <p>Run from the project root:
 * {@code java tools/GenerateBanner.java build/run/productionClientGameTest/screenshots docs}
 *
 * <p>The blossom is drawn with the same vector shapes as {@code GenerateAssets.java}. Banners are rendered at 2x and
 * box-filtered down for smooth edges.
 */
public final class GenerateBanner {
	private static final Color BACKGROUND = new Color(0xFFF7FA);
	private static final Color BACKGROUND_DEEP = new Color(0xFFD9E7);
	private static final Color PINK = new Color(0xF27BA5);
	private static final Color ROSE = new Color(0xD9578A);
	private static final Color TEXT = new Color(0x4A2C3A);
	private static final Color TEXT_SOFT = new Color(0x8B6676);

	private static final String TITLE = "Clicker";
	private static final String TAGLINE = "A Sakura-themed autoclicker for Minecraft 1.21.11";
	private static final String[] PILLS = {"Fabric", "1–100 CPS", "Toggle & Hold", "6 themes"};

	private static final String[] THEME_FILES = {"theme_sakura", "theme_midnight_sakura", "theme_blossom",
			"theme_rose", "theme_lavender", "theme_midnight"};
	private static final String[] THEME_NAMES = {"Sakura", "Midnight Sakura", "Blossom", "Rose", "Lavender", "Midnight"};

	/** Decorative petals: x, y (fractions of the canvas), size (fraction of height), angle (degrees), opacity. */
	private static final double[][] PETALS = {
			{0.64, 0.09, 0.075, 25, 0.38},
			{0.80, 0.12, 0.055, 100, 0.30},
			{0.93, 0.30, 0.095, -40, 0.45},
			{0.975, 0.72, 0.055, 10, 0.32},
			{0.90, 0.80, 0.070, 70, 0.36},
			{0.73, 0.91, 0.055, 140, 0.26},
			{0.53, 0.93, 0.045, -20, 0.22},
			{0.36, 0.06, 0.040, 30, 0.20},
			{0.05, 0.10, 0.050, -60, 0.24},
	};

	private GenerateBanner() {
	}

	public static void main(String[] args) throws IOException {
		Path screenshots = Path.of(args.length > 0 ? args[0] : "build/run/productionClientGameTest/screenshots");
		Path out = Path.of(args.length > 1 ? args[1] : "docs");
		Files.createDirectories(out);
		write(banner(1280, 400), out.resolve("banner.png"));
		write(banner(1280, 640), out.resolve("social-preview.png"));
		write(themes(screenshots), out.resolve("themes.png"));
	}

	// ------------------------------------------------------------------ banner

	private static BufferedImage banner(int width, int height) {
		int s = 2;
		int w = width * s;
		int h = height * s;
		BufferedImage img = canvas(w, h);
		Graphics2D g = graphics(img);
		double k = Math.min(height / 400.0, 1.3) * s;

		RoundRectangle2D card = new RoundRectangle2D.Double(0, 0, w, h, 80 * s, 80 * s);
		g.setPaint(new GradientPaint(0, 0, BACKGROUND, w, h, BACKGROUND_DEEP));
		g.fill(card);
		g.setClip(card);
		glow(g, w * 0.84, h * 0.12, h * 0.95, new Color(255, 255, 255, 170));
		glow(g, w * 0.10, h * 1.00, h * 0.85, new Color(255, 170, 200, 80));
		for (double[] p : PETALS) {
			drawLoosePetal(g, p[0] * w, p[1] * h, p[2] * h, p[3], p[4]);
		}
		g.setClip(null);
		g.setStroke(new BasicStroke(2f * s));
		g.setColor(new Color(242, 123, 165, 60));
		g.draw(new RoundRectangle2D.Double(s, s, w - 2.0 * s, h - 2.0 * s, 78 * s, 78 * s));

		// Logo with a soft halo.
		double petal = 118 * k;
		double logoX = Math.max(petal * 1.35, w * 0.17);
		double logoY = h / 2.0;
		glow(g, logoX, logoY, petal * 1.65, new Color(255, 255, 255, 230));
		drawColourBlossom(g, logoX, logoY, petal);

		// Text block, vertically centred; shrunk if the tagline would not fit beside the logo.
		double textX = logoX + petal * 1.55;
		double available = w - textX - 56 * s;
		double fit = Math.min(1.0, available / g.getFontMetrics(font("Segoe UI", Font.PLAIN, (float) (31 * k))).stringWidth(TAGLINE));
		Font titleFont = font("Segoe UI Semibold", Font.BOLD, (float) (124 * k * fit));
		Font taglineFont = font("Segoe UI", Font.PLAIN, (float) (31 * k * fit));
		Font pillFont = font("Segoe UI Semibold", Font.BOLD, (float) (21 * k * fit));
		k *= fit;
		FontMetrics title = g.getFontMetrics(titleFont);
		FontMetrics tagline = g.getFontMetrics(taglineFont);
		FontMetrics pill = g.getFontMetrics(pillFont);
		double pillHeight = pill.getAscent() + 18 * k;
		double gap1 = 2 * k;
		double gap2 = 24 * k;
		double titleHeight = title.getAscent() * 0.78;
		double blockHeight = titleHeight + gap1 + tagline.getHeight() + gap2 + pillHeight;
		double top = (h - blockHeight) / 2.0;

		g.setFont(titleFont);
		g.setColor(TEXT);
		double titleBaseline = top + titleHeight;
		g.drawString(TITLE, (float) textX, (float) titleBaseline);
		// A small accent bar under the wordmark.
		double titleWidth = title.stringWidth(TITLE);
		g.setPaint(new GradientPaint((float) textX, 0, PINK, (float) (textX + titleWidth), 0, ROSE));
		g.fill(new RoundRectangle2D.Double(textX + 4 * k, titleBaseline + 10 * k, titleWidth * 0.32, 6 * k, 6 * k, 6 * k));

		g.setFont(taglineFont);
		g.setColor(TEXT_SOFT);
		double taglineBaseline = titleBaseline + gap1 + tagline.getAscent() + 16 * k;
		g.drawString(TAGLINE, (float) textX, (float) taglineBaseline);

		double pillY = taglineBaseline + tagline.getDescent() + gap2;
		double pillX = textX;
		g.setFont(pillFont);
		for (String label : PILLS) {
			double pw = pill.stringWidth(label) + 34 * k;
			RoundRectangle2D shape = new RoundRectangle2D.Double(pillX, pillY, pw, pillHeight, pillHeight, pillHeight);
			g.setColor(new Color(255, 255, 255, 215));
			g.fill(shape);
			g.setStroke(new BasicStroke((float) (1.6 * k)));
			g.setColor(new Color(242, 123, 165, 110));
			g.draw(shape);
			g.setColor(ROSE);
			g.fill(new Ellipse2D.Double(pillX + 12 * k, pillY + pillHeight / 2 - 4 * k, 8 * k, 8 * k));
			g.drawString(label, (float) (pillX + 25 * k), (float) (pillY + (pillHeight + pill.getAscent() - pill.getDescent()) / 2));
			pillX += pw + 12 * k;
		}
		g.dispose();
		return downscale(img, s);
	}

	private static void glow(Graphics2D g, double cx, double cy, double radius, Color color) {
		Color clear = new Color(color.getRed(), color.getGreen(), color.getBlue(), 0);
		g.setPaint(new RadialGradientPaint(new Point2D.Double(cx, cy), (float) radius, new float[]{0f, 1f},
				new Color[]{color, clear}, MultipleGradientPaint.CycleMethod.NO_CYCLE));
		g.fill(new Ellipse2D.Double(cx - radius, cy - radius, radius * 2, radius * 2));
	}

	private static void drawLoosePetal(Graphics2D g, double cx, double cy, double size, double angle, double opacity) {
		AffineTransform t = new AffineTransform();
		t.translate(cx, cy);
		t.rotate(Math.toRadians(angle));
		t.translate(0, size * 0.48);
		Shape petal = t.createTransformedShape(petalAtOrigin(size, size * 0.78));
		int a = (int) Math.round(opacity * 255);
		g.setPaint(new GradientPaint((float) cx, (float) (cy - size / 2), new Color(255, 182, 207, a),
				(float) cx, (float) (cy + size / 2), new Color(242, 123, 165, a)));
		g.fill(petal);
	}

	// ------------------------------------------------------------------ blossom (same shapes as GenerateAssets)

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
		g.setStroke(new BasicStroke((float) (length * 0.035), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		for (int i = 0; i < 5; i++) {
			double a = Math.toRadians(i * 72.0 + 36.0 - 90.0);
			double r = length * 0.36;
			double x = cx + Math.cos(a) * r;
			double y = cy + Math.sin(a) * r;
			g.setColor(new Color(0xC2306A));
			g.draw(new Line2D.Double(cx, cy, x, y));
			double d = length * 0.085;
			g.setColor(new Color(0xFFD36E));
			g.fill(new Ellipse2D.Double(x - d / 2, y - d / 2, d, d));
		}
		double c = length * 0.2;
		g.setColor(new Color(0xB8285E));
		g.fill(new Ellipse2D.Double(cx - c / 2, cy - c / 2, c, c));
	}

	// ------------------------------------------------------------------ theme collage

	private static BufferedImage themes(Path screenshots) throws IOException {
		int tileW = 640;
		int tileH = 360;
		int gap = 16;
		int cols = 3;
		int rows = 2;
		int w = cols * tileW + (cols + 1) * gap;
		int h = rows * tileH + (rows + 1) * gap;
		BufferedImage img = canvas(w, h);
		Graphics2D g = graphics(img);
		g.setColor(BACKGROUND);
		g.fill(new RoundRectangle2D.Double(0, 0, w, h, 56, 56));

		Font labelFont = font("Segoe UI Semibold", Font.BOLD, 22f);
		FontMetrics label = g.getFontMetrics(labelFont);
		for (int i = 0; i < THEME_FILES.length; i++) {
			BufferedImage source = ImageIO.read(findScreenshot(screenshots, THEME_FILES[i]).toFile());
			BufferedImage tile = roundedTile(boxScale(source, tileW, tileH), 18);
			int x = gap + (i % cols) * (tileW + gap);
			int y = gap + (i / cols) * (tileH + gap);
			for (int layer = 3; layer >= 1; layer--) {
				g.setColor(new Color(176, 87, 122, 16));
				g.fill(new RoundRectangle2D.Double(x - layer, y + 3 - layer, tileW + 2 * layer, tileH + 2 * layer, 36 + 2 * layer, 36 + 2 * layer));
			}
			g.drawImage(tile, x, y, null);

			String name = THEME_NAMES[i];
			double pw = label.stringWidth(name) + 40;
			double ph = 38;
			double px = x + 16;
			double py = y + 16;
			g.setColor(new Color(255, 255, 255, 225));
			g.fill(new RoundRectangle2D.Double(px, py, pw, ph, ph, ph));
			g.setColor(ROSE);
			g.fill(new Ellipse2D.Double(px + 14, py + ph / 2 - 4, 8, 8));
			g.setFont(labelFont);
			g.setColor(TEXT);
			g.drawString(name, (float) (px + 28), (float) (py + (ph + label.getAscent() - label.getDescent()) / 2));
		}
		g.dispose();
		return img;
	}

	private static Path findScreenshot(Path dir, String suffix) throws IOException {
		try (var files = Files.list(dir)) {
			return files.filter(p -> p.getFileName().toString().endsWith("_" + suffix + ".png"))
					.sorted()
					.findFirst()
					.orElseThrow(() -> new IOException("No *_" + suffix + ".png in " + dir));
		}
	}

	private static BufferedImage roundedTile(BufferedImage image, int radius) {
		BufferedImage tile = canvas(image.getWidth(), image.getHeight());
		Graphics2D g = graphics(tile);
		g.setColor(Color.WHITE);
		g.fill(new RoundRectangle2D.Double(0, 0, image.getWidth(), image.getHeight(), radius * 2, radius * 2));
		g.setComposite(AlphaComposite.SrcIn);
		g.drawImage(image, 0, 0, null);
		g.dispose();
		return tile;
	}

	// ------------------------------------------------------------------ helpers

	private static Font font(String family, int fallbackStyle, float size) {
		boolean installed = Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()).contains(family);
		return installed ? new Font(family, Font.PLAIN, 1).deriveFont(size) : new Font(Font.SANS_SERIF, fallbackStyle, 1).deriveFont(size);
	}

	private static BufferedImage canvas(int w, int h) {
		return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
	}

	private static Graphics2D graphics(BufferedImage img) {
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		return g;
	}

	/** Area-averaging downscale to an exact size (premultiplied, so transparent edges stay clean). */
	private static BufferedImage boxScale(BufferedImage src, int outW, int outH) {
		BufferedImage out = canvas(outW, outH);
		double sx = (double) src.getWidth() / outW;
		double sy = (double) src.getHeight() / outH;
		for (int y = 0; y < outH; y++) {
			int y0 = (int) Math.floor(y * sy);
			int y1 = Math.max(y0 + 1, (int) Math.floor((y + 1) * sy));
			for (int x = 0; x < outW; x++) {
				int x0 = (int) Math.floor(x * sx);
				int x1 = Math.max(x0 + 1, (int) Math.floor((x + 1) * sx));
				double a = 0, r = 0, gg = 0, b = 0;
				int n = 0;
				for (int yy = y0; yy < y1; yy++) {
					for (int xx = x0; xx < x1; xx++) {
						int argb = src.getRGB(xx, yy);
						double pa = ((argb >>> 24) & 0xFF) / 255.0;
						a += pa;
						r += pa * ((argb >>> 16) & 0xFF);
						gg += pa * ((argb >>> 8) & 0xFF);
						b += pa * (argb & 0xFF);
						n++;
					}
				}
				int outA = (int) Math.round(a / n * 255.0);
				if (outA == 0) {
					continue;
				}
				out.setRGB(x, y, (outA << 24) | (clamp(r / a) << 16) | (clamp(gg / a) << 8) | clamp(b / a));
			}
		}
		return out;
	}

	private static BufferedImage downscale(BufferedImage src, int factor) {
		return boxScale(src, src.getWidth() / factor, src.getHeight() / factor);
	}

	private static int clamp(double v) {
		return (int) Math.max(0, Math.min(255, Math.round(v)));
	}

	private static void write(BufferedImage img, Path path) throws IOException {
		ImageIO.write(img, "png", path.toFile());
		System.out.println("  wrote " + path + " (" + img.getWidth() + "x" + img.getHeight() + ")");
	}
}
