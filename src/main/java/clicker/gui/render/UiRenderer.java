package clicker.gui.render;

import clicker.Clicker;
import clicker.gui.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2fStack;

import java.util.List;

/**
 * Clicker's 2D drawing toolkit.
 *
 * <p>Layout happens in "units" (like GUI pixels); one unit is {@link #scale()} physical pixels, where the scale is
 * always a whole number. Everything is drawn in physical pixel space so rounded corners and icons are anti-aliased
 * at full resolution, and text is drawn at whole-pixel sizes so Minecraft's font stays crisp.
 *
 * <p>All colours pass through a stackable alpha multiplier, which is how windows and pages fade.
 */
public final class UiRenderer {
	public static final Identifier LOGO_PETALS = Clicker.id("textures/gui/logo_petals.png");
	public static final Identifier LOGO_CENTER = Clicker.id("textures/gui/logo_center.png");
	public static final Identifier PETAL = Clicker.id("textures/gui/petal.png");
	public static final int LOGO_TEXTURE_SIZE = 128;
	public static final int PETAL_TEXTURE_SIZE = 64;

	private static final ShapeTextures SHAPES = new ShapeTextures();

	private final Minecraft minecraft = Minecraft.getInstance();
	private final float[] alphaStack = new float[64];
	private GuiGraphics graphics;
	private Font font;
	private float scale = 1f;
	private int guiScale = 1;
	private float alpha = 1f;
	private int alphaDepth;

	// ------------------------------------------------------------------ frame

	/** Starts drawing; {@code uiScale} is the number of physical pixels per unit. */
	public void begin(GuiGraphics graphics, int uiScale) {
		this.graphics = graphics;
		this.font = this.minecraft.font;
		this.scale = Math.max(1, uiScale);
		this.guiScale = Math.max(1, this.minecraft.getWindow().getGuiScale());
		this.alpha = 1f;
		this.alphaDepth = 0;
		graphics.pose().pushMatrix();
		graphics.pose().scale(1f / this.guiScale, 1f / this.guiScale);
	}

	public void end() {
		this.graphics.pose().popMatrix();
		this.graphics = null;
	}

	public GuiGraphics graphics() {
		return this.graphics;
	}

	public Font font() {
		return this.font;
	}

	public float scale() {
		return this.scale;
	}

	public int px(float units) {
		return Math.round(units * this.scale);
	}

	/** Width of one physical pixel in units. */
	public float pixel() {
		return 1f / this.scale;
	}

	// ------------------------------------------------------------------ alpha & transforms

	public void pushAlpha(float factor) {
		this.alphaStack[this.alphaDepth++] = this.alpha;
		this.alpha *= Math.max(0f, Math.min(1f, factor));
	}

	public void popAlpha() {
		this.alpha = this.alphaStack[--this.alphaDepth];
	}

	public float alpha() {
		return this.alpha;
	}

	public int tint(int argb) {
		return Colors.fade(argb, this.alpha);
	}

	public void pushOffset(float dxUnits, float dyUnits) {
		Matrix3x2fStack pose = this.graphics.pose();
		pose.pushMatrix();
		pose.translate(this.px(dxUnits), this.px(dyUnits));
	}

	public void popOffset() {
		this.graphics.pose().popMatrix();
	}

	public void pushScissor(float x, float y, float w, float h) {
		this.graphics.enableScissor(this.px(x), this.px(y), this.px(x + w), this.px(y + h));
	}

	public void popScissor() {
		this.graphics.disableScissor();
	}

	// ------------------------------------------------------------------ rectangles

	public void rect(float x, float y, float w, float h, int color) {
		int c = this.tint(color);
		if (Colors.alpha(c) == 0) {
			return;
		}
		int x0 = this.px(x);
		int y0 = this.px(y);
		int x1 = this.px(x + w);
		int y1 = this.px(y + h);
		if (x1 > x0 && y1 > y0) {
			this.graphics.fill(x0, y0, x1, y1, c);
		}
	}

	public void gradientV(float x, float y, float w, float h, int top, int bottom) {
		int x0 = this.px(x);
		int y0 = this.px(y);
		int x1 = this.px(x + w);
		int y1 = this.px(y + h);
		if (x1 > x0 && y1 > y0) {
			this.graphics.fillGradient(x0, y0, x1, y1, this.tint(top), this.tint(bottom));
		}
	}

	public void gradientH(float x, float y, float w, float h, int left, int right) {
		int x0 = this.px(x);
		int y0 = this.px(y);
		int x1 = this.px(x + w);
		int y1 = this.px(y + h);
		if (x1 <= x0 || y1 <= y0) {
			return;
		}
		Matrix3x2fStack pose = this.graphics.pose();
		pose.pushMatrix();
		pose.translate(x0, y1);
		pose.rotate((float) (-Math.PI / 2.0));
		this.graphics.fillGradient(0, 0, y1 - y0, x1 - x0, this.tint(left), this.tint(right));
		pose.popMatrix();
	}

	public void round(float x, float y, float w, float h, float radius, int color) {
		this.roundPx(this.px(x), this.px(y), this.px(x + w), this.px(y + h), this.px(radius), color);
	}

	public void roundPx(int x0, int y0, int x1, int y1, int radius, int color) {
		int c = this.tint(color);
		if (Colors.alpha(c) == 0 || x1 <= x0 || y1 <= y0) {
			return;
		}
		int r = Math.min(radius, Math.min(x1 - x0, y1 - y0) / 2);
		if (r <= 0) {
			this.graphics.fill(x0, y0, x1, y1, c);
			return;
		}
		Identifier circle = SHAPES.circle(r);
		int size = r * 2;
		this.blit(circle, x0, y0, 0, 0, r, r, size, size, c);
		this.blit(circle, x1 - r, y0, r, 0, r, r, size, size, c);
		this.blit(circle, x0, y1 - r, 0, r, r, r, size, size, c);
		this.blit(circle, x1 - r, y1 - r, r, r, r, r, size, size, c);
		if (x1 - r > x0 + r) {
			this.graphics.fill(x0 + r, y0, x1 - r, y1, c);
		}
		if (y1 - r > y0 + r) {
			this.graphics.fill(x0, y0 + r, x0 + r, y1 - r, c);
			this.graphics.fill(x1 - r, y0 + r, x1, y1 - r, c);
		}
	}

	/** Rounded rectangle with a vertical gradient. Best for shapes taller than their corner radius. */
	public void roundGradient(float x, float y, float w, float h, float radius, int top, int bottom) {
		int x0 = this.px(x);
		int y0 = this.px(y);
		int x1 = this.px(x + w);
		int y1 = this.px(y + h);
		if (x1 <= x0 || y1 <= y0) {
			return;
		}
		int r = Math.min(this.px(radius), Math.min(x1 - x0, y1 - y0) / 2);
		if (r <= 0) {
			this.graphics.fillGradient(x0, y0, x1, y1, this.tint(top), this.tint(bottom));
			return;
		}
		float height = y1 - y0;
		int topCorner = this.tint(Colors.lerp(top, bottom, (r * 0.5f) / height));
		int bottomCorner = this.tint(Colors.lerp(top, bottom, 1f - (r * 0.5f) / height));
		Identifier circle = SHAPES.circle(r);
		int size = r * 2;
		this.blit(circle, x0, y0, 0, 0, r, r, size, size, topCorner);
		this.blit(circle, x1 - r, y0, r, 0, r, r, size, size, topCorner);
		this.blit(circle, x0, y1 - r, 0, r, r, r, size, size, bottomCorner);
		this.blit(circle, x1 - r, y1 - r, r, r, r, r, size, size, bottomCorner);
		if (x1 - r > x0 + r) {
			this.graphics.fillGradient(x0 + r, y0, x1 - r, y1, this.tint(top), this.tint(bottom));
		}
		if (y1 - r > y0 + r) {
			int sideTop = this.tint(Colors.lerp(top, bottom, r / height));
			int sideBottom = this.tint(Colors.lerp(top, bottom, 1f - r / height));
			this.graphics.fillGradient(x0, y0 + r, x0 + r, y1 - r, sideTop, sideBottom);
			this.graphics.fillGradient(x1 - r, y0 + r, x1, y1 - r, sideTop, sideBottom);
		}
	}

	/** A rounded outline; {@code thicknessPx} is in physical pixels so hairlines stay hairlines. */
	public void outline(float x, float y, float w, float h, float radius, int thicknessPx, int color) {
		this.outlinePx(this.px(x), this.px(y), this.px(x + w), this.px(y + h), this.px(radius), thicknessPx, color);
	}

	public void outlinePx(int x0, int y0, int x1, int y1, int radius, int thickness, int color) {
		int c = this.tint(color);
		if (Colors.alpha(c) == 0 || x1 <= x0 || y1 <= y0) {
			return;
		}
		int t = Math.max(1, thickness);
		int r = Math.min(radius, Math.min(x1 - x0, y1 - y0) / 2);
		if (r < t) {
			this.graphics.fill(x0, y0, x1, y0 + t, c);
			this.graphics.fill(x0, y1 - t, x1, y1, c);
			this.graphics.fill(x0, y0 + t, x0 + t, y1 - t, c);
			this.graphics.fill(x1 - t, y0 + t, x1, y1 - t, c);
			return;
		}
		Identifier ring = SHAPES.ring(r, t);
		int size = r * 2;
		this.blit(ring, x0, y0, 0, 0, r, r, size, size, c);
		this.blit(ring, x1 - r, y0, r, 0, r, r, size, size, c);
		this.blit(ring, x0, y1 - r, 0, r, r, r, size, size, c);
		this.blit(ring, x1 - r, y1 - r, r, r, r, r, size, size, c);
		if (x1 - r > x0 + r) {
			this.graphics.fill(x0 + r, y0, x1 - r, y0 + t, c);
			this.graphics.fill(x0 + r, y1 - t, x1 - r, y1, c);
		}
		if (y1 - r > y0 + r) {
			this.graphics.fill(x0, y0 + r, x0 + t, y1 - r, c);
			this.graphics.fill(x1 - t, y0 + r, x1, y1 - r, c);
		}
	}

	/** A soft drop shadow for a rounded rectangle (draw it before the shape itself). */
	public void shadow(float x, float y, float w, float h, float radius, float blur, int color) {
		int x0 = this.px(x);
		int y0 = this.px(y);
		int x1 = this.px(x + w);
		int y1 = this.px(y + h);
		int minSide = Math.min(x1 - x0, y1 - y0);
		if (minSide <= 0 || Colors.alpha(this.tint(color)) == 0) {
			return;
		}
		int r = Math.min(this.px(radius), minSide / 2);
		int b = Math.max(1, this.px(blur));
		int maxBlur = (minSide - 2 * r) / 2;
		if (maxBlur >= 2) {
			this.nineSliceShadow(x0, y0, x1, y1, r, Math.min(b, maxBlur), color);
		} else {
			// Pills and tiny shapes: a few soft expanding layers.
			int layers = 3;
			for (int i = layers; i >= 1; i--) {
				int grow = Math.max(1, Math.round(b * i / (float) layers));
				this.roundPx(x0 - grow, y0 - grow, x1 + grow, y1 + grow, r + grow, Colors.fade(color, 0.24f));
			}
		}
	}

	private void nineSliceShadow(int x0, int y0, int x1, int y1, int r, int b, int color) {
		ShapeTextures.Shadow shadow = SHAPES.shadow(r, b);
		int c = this.tint(color);
		int ex0 = x0 - b;
		int ey0 = y0 - b;
		int ex1 = x1 + b;
		int ey1 = y1 + b;
		int corner = shadow.corner();
		int size = shadow.size();
		Identifier tex = shadow.texture();
		this.blit(tex, ex0, ey0, 0, 0, corner, corner, size, size, c);
		this.blit(tex, ex1 - corner, ey0, size - corner, 0, corner, corner, size, size, c);
		this.blit(tex, ex0, ey1 - corner, 0, size - corner, corner, corner, size, size, c);
		this.blit(tex, ex1 - corner, ey1 - corner, size - corner, size - corner, corner, corner, size, size, c);
		int innerW = ex1 - ex0 - 2 * corner;
		int innerH = ey1 - ey0 - 2 * corner;
		if (innerW > 0) {
			this.blitRegion(tex, ex0 + corner, ey0, corner, 0, innerW, corner, 1, corner, size, size, c);
			this.blitRegion(tex, ex0 + corner, ey1 - corner, corner, size - corner, innerW, corner, 1, corner, size, size, c);
		}
		if (innerH > 0) {
			this.blitRegion(tex, ex0, ey0 + corner, 0, corner, corner, innerH, corner, 1, size, size, c);
			this.blitRegion(tex, ex1 - corner, ey0 + corner, size - corner, corner, corner, innerH, corner, 1, size, size, c);
		}
		if (innerW > 0 && innerH > 0) {
			this.graphics.fill(ex0 + corner, ey0 + corner, ex1 - corner, ey1 - corner, c);
		}
	}

	/** A card: soft shadow, fill and hairline edge. */
	public void card(float x, float y, float w, float h, float radius, int fill, int edge, int shadow) {
		this.shadow(x, y + 1.5f, w, h, radius, 7f, shadow);
		this.round(x, y, w, h, radius, fill);
		this.outline(x, y, w, h, radius, 1, edge);
	}

	public void circle(float cx, float cy, float radius, int color) {
		int c = this.tint(color);
		if (Colors.alpha(c) == 0) {
			return;
		}
		int r = Math.max(1, this.px(radius));
		int size = r * 2;
		this.blit(SHAPES.circle(r), this.px(cx) - r, this.px(cy) - r, 0, 0, size, size, size, size, c);
	}

	public void ring(float cx, float cy, float radius, int thicknessPx, int color) {
		int c = this.tint(color);
		if (Colors.alpha(c) == 0) {
			return;
		}
		int r = Math.max(1, this.px(radius));
		int size = r * 2;
		this.blit(SHAPES.ring(r, thicknessPx), this.px(cx) - r, this.px(cy) - r, 0, 0, size, size, size, size, c);
	}

	// ------------------------------------------------------------------ images

	public void icon(Icon icon, float x, float y, float size, int color) {
		int c = this.tint(color);
		if (Colors.alpha(c) == 0) {
			return;
		}
		int s = Math.max(4, this.px(size));
		this.blit(SHAPES.icon(icon, s), this.px(x), this.px(y), 0, 0, s, s, s, s, c);
	}

	/** Draws a whole (smoothly filtered) texture into a box. */
	public void sprite(Identifier texture, int textureSize, float x, float y, float w, float h, int color) {
		int c = this.tint(color);
		if (Colors.alpha(c) == 0) {
			return;
		}
		this.graphics.blit(RenderPipelines.GUI_TEXTURED, texture, this.px(x), this.px(y), 0, 0,
				this.px(w), this.px(h), textureSize, textureSize, textureSize, textureSize, c);
	}

	public void spriteRotated(Identifier texture, int textureSize, float cx, float cy, float w, float h, float radians, int color) {
		int c = this.tint(color);
		if (Colors.alpha(c) == 0) {
			return;
		}
		Matrix3x2fStack pose = this.graphics.pose();
		pose.pushMatrix();
		pose.translate(cx * this.scale, cy * this.scale);
		pose.rotate(radians);
		int pw = Math.max(1, this.px(w));
		int ph = Math.max(1, this.px(h));
		this.graphics.blit(RenderPipelines.GUI_TEXTURED, texture, -pw / 2, -ph / 2, 0, 0, pw, ph,
				textureSize, textureSize, textureSize, textureSize, c);
		pose.popMatrix();
	}

	/** The Clicker blossom logo: petals in one colour, centre and stamens in another. */
	public void logo(float x, float y, float size, int petals, int center) {
		this.sprite(LOGO_PETALS, LOGO_TEXTURE_SIZE, x, y, size, size, petals);
		this.sprite(LOGO_CENTER, LOGO_TEXTURE_SIZE, x, y, size, size, center);
	}

	/** Renders an item icon; items cannot fade, so they are skipped while mostly transparent. */
	public void item(ItemStack stack, float x, float y, float size) {
		if (this.alpha < 0.5f || stack.isEmpty()) {
			return;
		}
		Matrix3x2fStack pose = this.graphics.pose();
		pose.pushMatrix();
		pose.translate(this.px(x), this.px(y));
		float k = this.px(size) / 16f;
		pose.scale(k, k);
		this.graphics.renderItem(stack, 0, 0);
		pose.popMatrix();
	}

	// ------------------------------------------------------------------ text

	/** Physical pixels per font pixel for a text size factor (1 = normal). */
	public int textPx(float factor) {
		return Math.max(1, Math.round(this.scale * factor));
	}

	/** Units per font pixel for a size factor. */
	public float textScale(float factor) {
		return this.textPx(factor) / this.scale;
	}

	public float width(Component text) {
		return this.font.width(text);
	}

	public float width(FormattedCharSequence text) {
		return this.font.width(text);
	}

	public float width(Component text, float factor) {
		return this.font.width(text) * this.textScale(factor);
	}

	/** Height of a line of text (cap height plus descenders) at a size factor. */
	public float lineHeight(float factor) {
		return 8f * this.textScale(factor);
	}

	public void text(Component text, float x, float y, int color) {
		this.drawText(text.getVisualOrderText(), x, y, color, this.textPx(1f));
	}

	public void text(FormattedCharSequence text, float x, float y, int color) {
		this.drawText(text, x, y, color, this.textPx(1f));
	}

	public void text(Component text, float x, float y, int color, float factor) {
		this.drawText(text.getVisualOrderText(), x, y, color, this.textPx(factor));
	}

	public void textCentered(Component text, float centerX, float y, int color) {
		this.text(text, centerX - this.width(text) / 2f, y, color);
	}

	public void textCentered(Component text, float centerX, float y, int color, float factor) {
		this.text(text, centerX - this.width(text, factor) / 2f, y, color, factor);
	}

	public void textRight(Component text, float rightX, float y, int color) {
		this.text(text, rightX - this.width(text), y, color);
	}

	public void textRight(Component text, float rightX, float y, int color, float factor) {
		this.text(text, rightX - this.width(text, factor), y, color, factor);
	}

	/** Shortens text with an ellipsis so it fits {@code maxWidth} units. */
	public Component ellipsize(Component text, float maxWidth) {
		if (this.width(text) <= maxWidth) {
			return text;
		}
		String ellipsis = "...";
		String plain = text.getString();
		int keep = Math.max(0, plain.length());
		while (keep > 0 && this.font.width(plain.substring(0, keep) + ellipsis) > maxWidth) {
			keep--;
		}
		return Component.literal(plain.substring(0, keep) + ellipsis).withStyle(text.getStyle());
	}

	public List<FormattedCharSequence> wrap(FormattedText text, float maxWidth) {
		return this.font.split(text, Math.max(1, (int) Math.floor(maxWidth)));
	}

	/** Draws wrapped text and returns the height used, in units. */
	public float paragraph(FormattedText text, float x, float y, float maxWidth, int color, float lineSpacing) {
		List<FormattedCharSequence> lines = this.wrap(text, maxWidth);
		float lineAdvance = 9f + lineSpacing;
		for (int i = 0; i < lines.size(); i++) {
			this.text(lines.get(i), x, y + i * lineAdvance, color);
		}
		return lines.isEmpty() ? 0f : lines.size() * lineAdvance - lineSpacing;
	}

	public float paragraphHeight(FormattedText text, float maxWidth, float lineSpacing) {
		int lines = this.wrap(text, maxWidth).size();
		return lines == 0 ? 0f : lines * (9f + lineSpacing) - lineSpacing;
	}

	private void drawText(FormattedCharSequence text, float x, float y, int color, int textPx) {
		int c = this.tint(color);
		if (Colors.alpha(c) < 4) {
			return;
		}
		Matrix3x2fStack pose = this.graphics.pose();
		pose.pushMatrix();
		pose.translate(this.px(x), this.px(y));
		pose.scale(textPx, textPx);
		this.graphics.drawString(this.font, text, 0, 0, c, false);
		pose.popMatrix();
	}

	// ------------------------------------------------------------------ helpers

	private void blit(Identifier texture, int x, int y, float u, float v, int w, int h, int textureW, int textureH, int color) {
		this.graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, w, h, textureW, textureH, color);
	}

	private void blitRegion(Identifier texture, int x, int y, float u, float v, int w, int h, int regionW, int regionH,
			int textureW, int textureH, int color) {
		this.graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, w, h, regionW, regionH, textureW, textureH, color);
	}
}
