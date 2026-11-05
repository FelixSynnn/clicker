package clicker.gui.components;

import clicker.gui.Palette;
import clicker.gui.render.Icon;
import clicker.gui.render.UiRenderer;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/**
 * A rounded card that hosts other widgets, with an optional header (icon badge, title and subtitle).
 */
public class RoundedPanel extends Container {
	public static final float PADDING = 10f;
	public static final float RADIUS = 9f;
	public static final float HEADER_HEIGHT = 26f;

	private @Nullable Icon icon;
	private @Nullable Supplier<Component> title;
	private @Nullable Supplier<Component> subtitle;

	public RoundedPanel header(@Nullable Icon icon, @Nullable Supplier<Component> title, @Nullable Supplier<Component> subtitle) {
		this.icon = icon;
		this.title = title;
		this.subtitle = subtitle;
		return this;
	}

	public boolean hasHeader() {
		return this.title != null;
	}

	/** Y coordinate where content below the header starts. */
	public float contentTop() {
		return this.y + (this.hasHeader() ? PADDING + HEADER_HEIGHT : PADDING);
	}

	public float contentLeft() {
		return this.x + PADDING;
	}

	public float contentWidth() {
		return this.w - PADDING * 2f;
	}

	@Override
	protected void draw(UiContext ctx) {
		UiRenderer r = ctx.r;
		Palette p = ctx.p;
		r.card(this.x, this.y, this.w, this.h, RADIUS, p.card(), p.cardEdge(), p.shadow());
		if (this.title != null) {
			float tx = this.x + PADDING;
			float ty = this.y + PADDING;
			if (this.icon != null) {
				r.round(tx, ty, 18f, 18f, 6f, p.accentSoft());
				r.icon(this.icon, tx + 3f, ty + 3f, 12f, p.accentStrong());
				tx += 25f;
			}
			if (this.subtitle != null) {
				r.text(this.title.get(), tx, ty, p.text());
				r.text(r.ellipsize(this.subtitle.get(), this.x + this.w - PADDING - tx - 30f), tx, ty + 10f, p.textMuted());
			} else {
				r.text(this.title.get(), tx, ty + 5f, p.text());
			}
		}
		this.drawExtras(ctx);
		this.drawChildren(ctx);
	}

	/** Hook for subclasses to draw custom content beneath the children. */
	protected void drawExtras(UiContext ctx) {
	}
}
