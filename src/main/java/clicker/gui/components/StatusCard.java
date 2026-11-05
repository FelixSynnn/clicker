package clicker.gui.components;

import clicker.gui.Colors;
import clicker.gui.Palette;
import clicker.gui.UiSounds;
import clicker.gui.render.Icon;
import clicker.gui.render.UiRenderer;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/** A dashboard card: icon, title, live status chip, a large value and a detail line. Clicking navigates. */
public final class StatusCard extends Widget {
	public static final float HEIGHT = 60f;

	public enum Kind {
		ACTIVE,
		READY,
		PAUSED,
		OFF
	}

	public record Status(Component label, Kind kind) {
	}

	private final Icon icon;
	private final Supplier<Component> title;
	private final Supplier<Component> value;
	private final Supplier<Component> unit;
	private final Supplier<Component> detail;
	private final Supplier<@Nullable Status> status;
	private final @Nullable Runnable onClick;

	public StatusCard(Icon icon, Supplier<Component> title, Supplier<Component> value, Supplier<Component> unit,
			Supplier<Component> detail, Supplier<@Nullable Status> status, @Nullable Runnable onClick) {
		this.icon = icon;
		this.title = title;
		this.value = value;
		this.unit = unit;
		this.detail = detail;
		this.status = status;
		this.onClick = onClick;
		this.h = HEIGHT;
	}

	@Override
	protected void draw(UiContext ctx) {
		UiRenderer r = ctx.r;
		Palette p = ctx.p;
		float lift = this.onClick != null ? this.hover * 1.2f : 0f;
		float top = this.y - lift;
		r.shadow(this.x, top + 1.5f + lift * 0.6f, this.w, this.h, 9f, 7f + this.hover * 3f, p.shadow());
		r.round(this.x, top, this.w, this.h, 9f, p.card());
		r.outline(this.x, top, this.w, this.h, 9f, 1, Colors.lerp(p.cardEdge(), p.accent(), this.onClick != null ? this.hover * 0.5f : 0f));

		r.round(this.x + 10f, top + 10f, 18f, 18f, 6f, p.accentSoft());
		r.icon(this.icon, this.x + 13f, top + 13f, 12f, p.accentStrong());

		r.text(r.ellipsize(this.title.get(), this.w - 44f), this.x + 34f, top + 15f, p.textSecondary());

		float big = r.textScale(1.5f);
		float valueY = top + 32f;
		Status s = this.status.get();
		float valueRight = this.x + this.w - 10f;
		if (s != null) {
			// The live status chip sits on the value row, aligned with the bottom of the big number.
			valueRight = drawChip(r, p, s, valueRight, valueY + 8f * big - 11f) - 6f;
		}
		Component valueText = r.ellipsize(this.value.get(), (valueRight - this.x - 10f) / big);
		r.text(valueText, this.x + 10f, valueY, p.text(), 1.5f);
		float unitX = this.x + 10f + r.width(valueText, 1.5f) + 3f;
		if (unitX + r.width(this.unit.get()) <= valueRight) {
			r.text(this.unit.get(), unitX, valueY + 8f * big - 8f, p.textMuted());
		}

		r.text(r.ellipsize(this.detail.get(), this.w - 20f), this.x + 10f, top + this.h - 13f, p.textMuted());
	}

	/** Draws a small status pill ending at {@code right}; returns its left edge. */
	public static float drawChip(UiRenderer r, Palette p, Status s, float right, float y) {
		int bg;
		int fg;
		switch (s.kind()) {
			case ACTIVE -> {
				bg = p.accent();
				fg = p.onAccent();
			}
			case READY -> {
				bg = p.accentSoft();
				fg = p.accentStrong();
			}
			case PAUSED -> {
				bg = Colors.fade(p.warning(), 0.18f);
				fg = p.warning();
			}
			default -> {
				bg = p.track();
				fg = p.textMuted();
			}
		}
		float textWidth = r.width(s.label());
		float chipW = textWidth + 16f;
		float chipH = 12f;
		float left = right - chipW;
		r.round(left, y, chipW, chipH, chipH / 2f, bg);
		float pulse = s.kind() == Kind.ACTIVE ? 0.55f + 0.45f * (float) Math.sin(System.nanoTime() / 1.6e8) : 1f;
		r.circle(left + 6f, y + chipH / 2f, 1.8f, Colors.fade(fg, pulse));
		r.text(s.label(), left + 10f, y + 2.5f, fg);
		return left;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0 && this.onClick != null && this.contains(mouseX, mouseY)) {
			UiSounds.navigate();
			this.onClick.run();
			return true;
		}
		return false;
	}
}
