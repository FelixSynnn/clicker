package clicker.gui;

import clicker.Clicker;
import clicker.gui.anim.Anim;
import clicker.gui.components.UiContext;
import clicker.gui.render.UiRenderer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/** The left navigation rail: brand, page list with a gliding selection indicator, and version footer. */
public final class ClickerSidebar {
	private static final float ITEM_HEIGHT = 22f;
	private static final float ITEM_GAP = 2f;
	private static final float ITEMS_TOP = 44f;
	private static final Component BRAND = Component.literal(Clicker.NAME).withStyle(ChatFormatting.BOLD);

	private final ClickerScreen screen;
	private final float[] hover = new float[Page.values().length];
	private float x;
	private float y;
	private float w;
	private float h;
	private float indicatorY = Float.NaN;

	ClickerSidebar(ClickerScreen screen) {
		this.screen = screen;
	}

	void layout(float x, float y, float w, float h) {
		this.x = x;
		this.y = y;
		this.w = w;
		this.h = h;
	}

	private float itemY(int index) {
		return this.y + ITEMS_TOP + index * (ITEM_HEIGHT + ITEM_GAP);
	}

	private int itemAt(double mouseX, double mouseY) {
		if (mouseX < this.x + 4f || mouseX >= this.x + this.w - 4f) {
			return -1;
		}
		for (int i = 0; i < Page.values().length; i++) {
			float iy = this.itemY(i);
			if (mouseY >= iy && mouseY < iy + ITEM_HEIGHT) {
				return i;
			}
		}
		return -1;
	}

	void render(UiContext ctx, Page current) {
		UiRenderer r = ctx.r;
		Palette p = ctx.p;
		r.round(this.x, this.y, this.w, this.h, 10f, p.sidebar());

		r.logo(this.x + 9f, this.y + 9f, 20f, p.accent(), p.accentStrong());
		r.text(BRAND, this.x + 34f, this.y + 11f, p.text());
		r.text(r.ellipsize(Component.translatable("clicker.gui.tagline"), this.w - 40f), this.x + 34f, this.y + 21f, p.textMuted());

		float target = this.itemY(current.ordinal());
		if (Float.isNaN(this.indicatorY)) {
			this.indicatorY = target;
		}
		this.indicatorY = Anim.approach(this.indicatorY, target, ctx.dt, 15f);
		float ix = this.x + 6f;
		float iw = this.w - 12f;
		r.shadow(ix, this.indicatorY + 1f, iw, ITEM_HEIGHT, 7f, 4f, p.shadow());
		r.round(ix, this.indicatorY, iw, ITEM_HEIGHT, 7f, p.navActive());
		r.round(ix + 2f, this.indicatorY + 6f, 2.5f, ITEM_HEIGHT - 12f, 1.25f, p.accent());

		int hovered = ctx.hoverEnabled ? this.itemAt(ctx.mouseX, ctx.mouseY) : -1;
		Page[] pages = Page.values();
		for (int i = 0; i < pages.length; i++) {
			Page page = pages[i];
			float iy = this.itemY(i);
			this.hover[i] = Anim.approach(this.hover[i], i == hovered ? 1f : 0f, ctx.dt, 16f);
			if (page != current && this.hover[i] > 0.01f) {
				r.round(ix, iy, iw, ITEM_HEIGHT, 7f, Colors.fade(p.hover(), this.hover[i]));
			}
			float closeness = 1f - Math.min(1f, Math.abs(this.indicatorY - iy) / (ITEM_HEIGHT + ITEM_GAP));
			int color = Colors.lerp(Colors.lerp(p.textSecondary(), p.text(), this.hover[i]), p.accentStrong(), closeness);
			float nudge = this.hover[i] * 1f;
			r.icon(page.icon(), ix + 9f + nudge, iy + 5f, 12f, color);
			Component title = r.ellipsize(Component.translatable(page.titleKey()), iw - 32f);
			r.text(title, ix + 27f + nudge, iy + 7f, color);
		}

		Component footer = Component.translatable("clicker.gui.footer", Clicker.version());
		r.text(r.ellipsize(footer, this.w - 18f), this.x + 10f, this.y + this.h - 15f, p.textMuted());
	}

	boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button != 0 || mouseX < this.x || mouseX >= this.x + this.w || mouseY < this.y || mouseY >= this.y + this.h) {
			return false;
		}
		int index = this.itemAt(mouseX, mouseY);
		if (index >= 0) {
			Page page = Page.values()[index];
			if (page != this.screen.page()) {
				UiSounds.navigate();
				this.screen.navigate(page);
			}
		}
		return true;
	}
}
