package clicker.gui.components;

import clicker.gui.ClickerTheme;
import clicker.gui.Colors;
import clicker.gui.Palette;
import clicker.gui.UiSounds;
import clicker.gui.anim.Anim;
import clicker.gui.anim.Easing;
import clicker.gui.render.Icon;
import clicker.gui.render.UiRenderer;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;

/** A theme preset preview: a miniature Clicker window painted in that theme's colours. */
public final class ThemeCard extends Widget {
	public static final float HEIGHT = 80f;

	private final ClickerTheme theme;
	private final BooleanSupplier selected;
	private final Runnable onSelect;
	private float selection = -1f;

	public ThemeCard(ClickerTheme theme, BooleanSupplier selected, Runnable onSelect) {
		this.theme = theme;
		this.selected = selected;
		this.onSelect = onSelect;
		this.h = HEIGHT;
	}

	@Override
	protected void draw(UiContext ctx) {
		UiRenderer r = ctx.r;
		Palette p = ctx.p;
		Palette t = this.theme.palette();
		boolean isSelected = this.selected.getAsBoolean();
		if (this.selection < 0f) {
			this.selection = isSelected ? 1f : 0f;
		}
		this.selection = Anim.approach(this.selection, isSelected ? 1f : 0f, ctx.dt, 14f);
		float lift = this.hover * 1.5f;
		float top = this.y - lift;

		r.shadow(this.x, top + 1.5f + lift * 0.5f, this.w, this.h, 9f, 7f + this.hover * 3f, p.shadow());
		r.round(this.x, top, this.w, this.h, 9f, p.card());
		r.outline(this.x, top, this.w, this.h, 9f, 1, p.cardEdge());

		// Miniature window.
		float px = this.x + 6f;
		float py = top + 6f;
		float pw = this.w - 12f;
		float ph = 46f;
		r.round(px, py, pw, ph, 6f, t.window());
		r.outline(px, py, pw, ph, 6f, 1, t.windowEdge());
		float sideW = Math.min(24f, pw * 0.3f);
		r.round(px + 2.5f, py + 2.5f, sideW, ph - 5f, 4.5f, t.sidebar());
		r.logo(px + 5f, py + 5f, 7f, t.accent(), t.accentStrong());
		for (int i = 0; i < 3; i++) {
			float ny = py + 16f + i * 8f;
			if (i == 0) {
				r.round(px + 4.5f, ny - 1.5f, sideW - 4f, 6f, 3f, t.navActive());
				r.round(px + 6.5f, ny, 3f, 3f, 1.5f, t.accent());
			}
			r.round(px + 11.5f, ny + 0.5f, sideW - 15f, 2f, 1f, i == 0 ? t.accentStrong() : t.textMuted());
		}
		float cx = px + sideW + 6f;
		float cw = pw - sideW - 9f;
		r.round(cx, py + 5f, cw * 0.6f, 3f, 1.5f, t.text());
		r.round(cx, py + 11f, cw * 0.4f, 2f, 1f, t.textMuted());
		r.round(cx, py + 18f, cw, ph - 22f, 4f, t.card());
		r.outline(cx, py + 18f, cw, ph - 22f, 4f, 1, t.cardEdge());
		// Toggle and slider inside the mini card.
		r.round(cx + cw - 14f, py + 22f, 10f, 6f, 3f, t.accent());
		r.circle(cx + cw - 7f, py + 25f, 2f, t.knob());
		r.round(cx + 3.5f, py + 23.5f, cw * 0.36f, 2f, 1f, t.textSecondary());
		r.round(cx + 3.5f, py + 33f, cw - 7f, 2f, 1f, t.track());
		r.round(cx + 3.5f, py + 33f, (cw - 7f) * 0.62f, 2f, 1f, t.accent());
		r.circle(cx + 3.5f + (cw - 7f) * 0.62f, py + 34f, 2.2f, t.knob());

		Component name = Component.translatable(this.theme.translationKey());
		r.text(r.ellipsize(name, this.w - 14f), this.x + 7f, top + this.h - 21.5f, p.text());
		Component tone = Component.translatable(this.theme.dark() ? "clicker.theme.tone.dark" : "clicker.theme.tone.light");
		r.text(tone, this.x + 7f, top + this.h - 11.5f, p.textMuted());

		// Accent swatches on the tone line.
		float sx = this.x + this.w - 10f;
		float sy = top + this.h - 8f;
		int[] swatches = {t.accentStrong(), t.accent(), t.window()};
		for (int i = 0; i < swatches.length; i++) {
			float cxs = sx - i * 7f;
			r.circle(cxs, sy, 3.2f, Colors.over(p.card(), p.cardEdge()));
			r.circle(cxs, sy, 2.6f, swatches[i]);
		}

		if (this.selection > 0.01f) {
			r.pushAlpha(this.selection);
			r.outline(this.x - 1.5f, top - 1.5f, this.w + 3f, this.h + 3f, 10.5f, Math.max(2, r.px(1.2f)), p.accent());
			float pop = Easing.outBack(this.selection);
			float badge = 11f * pop;
			float bx = this.x + this.w - 4f - badge / 2f;
			float by = top + 4f - badge / 2f + 5.5f;
			r.circle(bx, by, badge / 2f + 1f, p.card());
			r.circle(bx, by, badge / 2f, p.accent());
			r.icon(Icon.CHECK, bx - badge * 0.36f, by - badge * 0.36f, badge * 0.72f, p.onAccent());
			r.popAlpha();
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0 && this.contains(mouseX, mouseY)) {
			if (!this.selected.getAsBoolean()) {
				UiSounds.select();
				this.onSelect.run();
			}
			return true;
		}
		return false;
	}
}
