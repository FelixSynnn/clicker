package clicker.gui.components;

import clicker.gui.Colors;
import clicker.gui.Palette;
import clicker.gui.UiSounds;
import clicker.gui.anim.Anim;
import clicker.gui.render.Icon;
import clicker.gui.render.UiRenderer;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/** A compact rounded button with optional icon and a soft hover/press response. */
public final class RoundedButton extends Widget {
	public static final float HEIGHT = 16f;

	public enum Style {
		PRIMARY,
		SECONDARY,
		DANGER,
		GHOST
	}

	private final Supplier<Component> label;
	private final @Nullable Icon icon;
	private final Runnable action;
	private final Style style;
	private float press;

	public RoundedButton(Supplier<Component> label, @Nullable Icon icon, Style style, Runnable action) {
		this.label = label;
		this.icon = icon;
		this.style = style;
		this.action = action;
		this.h = HEIGHT;
	}

	/** Natural width for the current label. */
	public float preferredWidth(UiRenderer r) {
		return r.width(this.label.get()) + (this.icon != null ? 14f : 0f) + 16f;
	}

	@Override
	protected void draw(UiContext ctx) {
		UiRenderer r = ctx.r;
		Palette p = ctx.p;
		this.press = Anim.approach(this.press, 0f, ctx.dt, 12f);
		float radius = Math.min(6f, this.h / 2f);
		float lift = -this.hover * 0.5f + this.press * 0.8f;

		int fill;
		int text;
		switch (this.style) {
			case PRIMARY -> {
				fill = Colors.lerp(p.accent(), p.accentStrong(), this.hover * 0.55f);
				text = p.onAccent();
				r.shadow(this.x, this.y + 1.2f + lift, this.w, this.h, radius, 4f, Colors.fade(p.accentStrong(), 0.35f + this.hover * 0.15f));
			}
			case DANGER -> {
				fill = Colors.lerp(Colors.fade(p.danger(), 0.13f), Colors.fade(p.danger(), 0.24f), this.hover);
				text = p.danger();
			}
			case GHOST -> {
				fill = Colors.fade(p.hover(), this.hover);
				text = Colors.lerp(p.textSecondary(), p.text(), this.hover);
			}
			default -> {
				fill = Colors.lerp(p.accentSoft(), Colors.lerp(p.accentSoft(), p.accent(), 0.25f), this.hover);
				text = p.accentStrong();
			}
		}
		r.round(this.x, this.y + lift, this.w, this.h, radius, fill);

		Component label = r.ellipsize(this.label.get(), this.w - (this.icon != null ? 22f : 8f));
		float contentWidth = r.width(label) + (this.icon != null ? 13f : 0f);
		float cx = this.x + (this.w - contentWidth) / 2f;
		float cy = this.y + lift + this.h / 2f;
		if (this.icon != null) {
			r.icon(this.icon, cx, cy - 5f, 10f, text);
			cx += 13f;
		}
		r.text(label, cx, cy - 4f, text);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0 && this.isActive() && this.contains(mouseX, mouseY)) {
			this.press = 1f;
			UiSounds.click();
			this.action.run();
			return true;
		}
		return false;
	}
}
