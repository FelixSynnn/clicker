package clicker.gui.hud;

import clicker.Clicker;
import clicker.ClickerClient;
import clicker.clicking.ClickType;
import clicker.clicking.ClickerManager;
import clicker.config.ClickerConfig;
import clicker.config.HudMode;
import clicker.gui.ClickerScreen;
import clicker.gui.Colors;
import clicker.gui.Palette;
import clicker.gui.anim.Anim;
import clicker.gui.render.UiRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * A small status pill in the top-left corner: shows whether Clicker is on and what each side is doing.
 * Hidden with F1, while the debug screen is open, and while the Clicker GUI itself is open.
 */
public final class ClickerHud implements HudElement {
	private static final Component BRAND = Component.literal(Clicker.NAME).withStyle(ChatFormatting.BOLD);

	private final UiRenderer renderer = new UiRenderer();
	private float visibility;
	private long lastFrame;

	@Override
	public void render(GuiGraphics graphics, DeltaTracker tickCounter) {
		Minecraft minecraft = Minecraft.getInstance();
		long now = System.nanoTime();
		float dt = this.lastFrame == 0L ? 0f : Math.min(0.1f, (now - this.lastFrame) / 1_000_000_000f);
		this.lastFrame = now;

		ClickerConfig cfg = ClickerClient.config().get();
		ClickerManager manager = ClickerClient.manager();
		boolean wanted = cfg.hud == HudMode.ALWAYS || (cfg.hud == HudMode.WHEN_ACTIVE && manager.isGlobalEnabled());
		boolean hidden = minecraft.options.hideGui || minecraft.getDebugOverlay().showDebugScreen() || ClickerScreen.isOpen();
		this.visibility = Anim.approach(this.visibility, wanted && !hidden ? 1f : 0f, dt, 12f);
		if (this.visibility < 0.02f) {
			return;
		}

		Palette p = ClickerClient.themes().palette();
		UiRenderer r = this.renderer;
		r.begin(graphics, minecraft.getWindow().getGuiScale());
		try {
			r.pushAlpha(this.visibility);
			float x = 5f;
			float y = 5f - (1f - this.visibility) * 4f;
			float h = 15f;

			boolean on = manager.isGlobalEnabled();
			float width = 22f + r.width(BRAND) + 6f;
			Chip left = chip(r, p, manager, cfg, ClickType.LEFT, on);
			Chip right = chip(r, p, manager, cfg, ClickType.RIGHT, on);
			Component off = Component.translatable("clicker.status.off");
			if (!on) {
				width += r.width(off) + 12f;
			} else {
				if (left != null) {
					width += left.width + 3f;
				}
				if (right != null) {
					width += right.width + 3f;
				}
			}
			width += 3f;

			r.shadow(x, y + 1f, width, h, h / 2f, 4f, p.shadow());
			r.round(x, y, width, h, h / 2f, Colors.fade(p.window(), 0.94f));
			r.outline(x, y, width, h, h / 2f, 1, p.windowEdge());
			r.logo(x + 4f, y + 2f, 11f, on ? p.accent() : p.textMuted(), on ? p.accentStrong() : p.textMuted());
			float cx = x + 18f;
			r.text(BRAND, cx, y + 4f, p.text());
			cx += r.width(BRAND) + 6f;

			if (!on) {
				r.round(cx, y + 2f, r.width(off) + 9f, h - 4f, (h - 4f) / 2f, p.track());
				r.text(off, cx + 4.5f, y + 4f, p.textMuted());
			} else {
				if (left != null) {
					cx = left.draw(r, cx, y + 2f, h - 4f) + 3f;
				}
				if (right != null) {
					right.draw(r, cx, y + 2f, h - 4f);
				}
			}
			r.popAlpha();
		} finally {
			r.end();
		}
	}

	private static Chip chip(UiRenderer r, Palette p, ClickerManager manager, ClickerConfig cfg, ClickType type, boolean on) {
		if (!on || !cfg.side(type).enabled) {
			return null;
		}
		ClickerManager.Side side = manager.side(type);
		String letter = type == ClickType.LEFT ? "L" : "R";
		Component label;
		int bg;
		int fg;
		boolean pulse = false;
		switch (side.status()) {
			case CLICKING -> {
				label = Component.literal(letter + " " + cfg.side(type).cps);
				bg = p.accent();
				fg = p.onAccent();
				pulse = true;
			}
			case PROTECTED -> {
				label = Component.translatable("clicker.hud.paused", letter);
				bg = Colors.fade(p.warning(), 0.22f);
				fg = p.warning();
			}
			default -> {
				label = Component.literal(letter + " " + cfg.side(type).cps);
				bg = p.accentSoft();
				fg = p.accentStrong();
			}
		}
		return new Chip(label, bg, fg, pulse, r.width(label) + 14f);
	}

	private record Chip(Component label, int bg, int fg, boolean pulse, float width) {
		float draw(UiRenderer r, float x, float y, float h) {
			r.round(x, y, this.width, h, h / 2f, this.bg);
			float glow = this.pulse ? 0.55f + 0.45f * (float) Math.sin(System.nanoTime() / 1.4e8) : 1f;
			r.circle(x + 5f, y + h / 2f, 1.7f, Colors.fade(this.fg, glow));
			r.text(this.label, x + 9f, y + 2f, this.fg);
			return x + this.width;
		}
	}
}
