package clicker.gui.pages;

import clicker.ClickerClient;
import clicker.clicking.ClickType;
import clicker.config.ClickerConfig;
import clicker.gui.ClickerScreen;
import clicker.gui.ClickerTheme;
import clicker.gui.Colors;
import clicker.gui.Page;
import clicker.gui.Palette;
import clicker.gui.StatusText;
import clicker.gui.UiSounds;
import clicker.gui.anim.Anim;
import clicker.gui.anim.Easing;
import clicker.gui.components.StatusCard;
import clicker.gui.components.UiContext;
import clicker.gui.components.Widget;
import clicker.gui.render.Icon;
import clicker.gui.render.UiRenderer;
import clicker.protection.ItemProtection;
import clicker.protection.ProtectedCategory;
import net.minecraft.network.chat.Component;

/** Overview: a big on/off hero card and live status cards for each feature. */
public final class HomePage extends PageView {
	private static final float HERO_HEIGHT = 64f;

	private final Hero hero;
	private final StatusCard left;
	private final StatusCard right;
	private final StatusCard protection;
	private final StatusCard theme;

	public HomePage(ClickerScreen screen) {
		super(screen);
		this.hero = this.add(new Hero());
		this.left = this.add(sideCard(screen, ClickType.LEFT));
		this.right = this.add(sideCard(screen, ClickType.RIGHT));
		this.protection = this.add(new StatusCard(Icon.SHIELD,
				() -> Component.translatable("clicker.home.protection"),
				() -> Component.literal(Integer.toString(enabledCategories())),
				() -> Component.translatable("clicker.home.protection_unit", ProtectedCategory.values().length),
				HomePage::protectionDetail,
				HomePage::protectionStatus,
				() -> screen.navigate(Page.PROTECTION)));
		this.theme = this.add(new StatusCard(Icon.PALETTE,
				() -> Component.translatable("clicker.home.theme"),
				() -> Component.translatable(ClickerClient.themes().current().translationKey()),
				Component::empty,
				() -> Component.translatable("clicker.home.theme_detail", ClickerTheme.PRESETS.size()),
				() -> null,
				() -> screen.navigate(Page.THEMES)));
	}

	private static StatusCard sideCard(ClickerScreen screen, ClickType type) {
		return new StatusCard(type == ClickType.LEFT ? Icon.MOUSE_LEFT : Icon.MOUSE_RIGHT,
				() -> Component.translatable(type.translationKey()),
				() -> Component.literal(Integer.toString(config().side(type).cps)),
				() -> Component.translatable("clicker.unit.cps"),
				() -> StatusText.sideDetail(type),
				() -> StatusText.side(type),
				() -> screen.navigate(Page.CLICKING));
	}

	private static int enabledCategories() {
		ClickerConfig cfg = config();
		int count = 0;
		for (ProtectedCategory category : ProtectedCategory.values()) {
			if (category.isEnabled(cfg)) {
				count++;
			}
		}
		return count;
	}

	private static Component protectionDetail() {
		if (!config().protectionEnabled) {
			return Component.translatable("clicker.home.protection_off");
		}
		ItemProtection.Match match = manager().protectionMatch();
		if (match != null) {
			return Component.translatable("clicker.home.protection_pausing", match.stack().getHoverName());
		}
		return Component.translatable("clicker.home.protection_detail");
	}

	private static StatusCard.Status protectionStatus() {
		if (!config().protectionEnabled) {
			return new StatusCard.Status(Component.translatable("clicker.status.off"), StatusCard.Kind.OFF);
		}
		if (manager().protectionMatch() != null) {
			return new StatusCard.Status(Component.translatable("clicker.status.pausing"), StatusCard.Kind.PAUSED);
		}
		return new StatusCard.Status(Component.translatable("clicker.status.on"), StatusCard.Kind.READY);
	}

	@Override
	public void layout(UiRenderer r, float x, float y, float width) {
		this.hero.bounds(x, y, width, HERO_HEIGHT);
		float top = y + HERO_HEIGHT + GAP;
		boolean twoColumns = width >= 240f;
		float col = twoColumns ? (width - GAP) / 2f : width;
		float h = StatusCard.HEIGHT;
		if (twoColumns) {
			this.left.bounds(x, top, col, h);
			this.right.bounds(x + col + GAP, top, col, h);
			this.protection.bounds(x, top + h + GAP, col, h);
			this.theme.bounds(x + col + GAP, top + h + GAP, col, h);
			this.contentHeight = HERO_HEIGHT + GAP + h * 2 + GAP;
		} else {
			this.left.bounds(x, top, col, h);
			this.right.bounds(x, top + (h + GAP), col, h);
			this.protection.bounds(x, top + (h + GAP) * 2, col, h);
			this.theme.bounds(x, top + (h + GAP) * 3, col, h);
			this.contentHeight = HERO_HEIGHT + GAP + h * 4 + GAP * 3;
		}
	}

	/** The large on/off card. Clicking it toggles Clicker, just like the G key. */
	private final class Hero extends Widget {
		private float on = -1f;

		@Override
		protected void draw(UiContext ctx) {
			UiRenderer r = ctx.r;
			Palette p = ctx.p;
			boolean enabled = manager().isGlobalEnabled();
			if (this.on < 0f) {
				this.on = enabled ? 1f : 0f;
			}
			this.on = Anim.approach(this.on, enabled ? 1f : 0f, ctx.dt, 9f);
			float t = Easing.smoothstep(this.on);

			r.shadow(this.x, this.y + 2f, this.w, this.h, 11f, 9f + this.hover * 3f, p.shadow());
			r.roundGradient(this.x, this.y, this.w, this.h, 11f, Colors.lerp(p.card(), p.accentSoft(), 0.35f + 0.65f * t), p.card());
			r.outline(this.x, this.y, this.w, this.h, 11f, 1, Colors.lerp(p.cardEdge(), p.accent(), 0.25f * t + this.hover * 0.25f));

			// A large, faint blossom tucked into the corner.
			r.pushScissor(this.x, this.y, this.w, this.h);
			r.pushAlpha(0.10f + 0.10f * t);
			r.logo(this.x + this.w - 64f, this.y - 10f, 84f, p.accent(), p.accentStrong());
			r.popAlpha();
			r.popScissor();

			float cx = this.x + 32f;
			float cy = this.y + this.h / 2f;
			float pulse = enabled ? 0.5f + 0.5f * (float) Math.sin(System.nanoTime() / 2.4e8) : 0f;
			float radius = 17f + this.hover * 0.8f;
			if (t > 0.01f) {
				r.circle(cx, cy, radius + 3.5f + pulse * 1.5f, Colors.fade(p.accent(), 0.18f * t));
			}
			r.shadow(cx - radius, cy - radius + 1.5f, radius * 2f, radius * 2f, radius, 3f, p.shadow());
			r.circle(cx, cy, radius, Colors.lerp(p.track(), p.accent(), t));
			r.icon(Icon.POWER, cx - 8f, cy - 8f, 16f, Colors.lerp(p.textMuted(), p.onAccent(), t));

			float tx = this.x + 60f;
			Component title = Component.translatable(enabled ? "clicker.home.title_on" : "clicker.home.title_off");
			r.text(title, tx, this.y + 15f, p.text(), 1.25f);
			Component key = ClickerClient.keybinds().toggle().getTranslatedKeyMessage();
			Component hint = Component.translatable(enabled ? "clicker.home.hint_on" : "clicker.home.hint_off", key);
			float hintY = this.y + 15f + r.lineHeight(1.25f) + 5f;
			r.text(r.ellipsize(hint, this.w - 70f), tx, hintY, p.textSecondary());
			Component summary = Component.translatable("clicker.home.summary",
					sideSummary(ClickType.LEFT), sideSummary(ClickType.RIGHT));
			r.text(r.ellipsize(summary, this.w - 70f), tx, hintY + 11f, p.textMuted());

			StatusCard.drawChip(r, p, StatusText.global(), this.x + this.w - 10f, this.y + 10f);
		}

		private Component sideSummary(ClickType type) {
			ClickerConfig.SideConfig side = config().side(type);
			if (!side.enabled) {
				return Component.translatable("clicker.home.summary_off", Component.translatable(type.translationKey()));
			}
			return Component.translatable("clicker.home.summary_side", Component.translatable(type.translationKey()), side.cps);
		}

		@Override
		public boolean mouseClicked(double mouseX, double mouseY, int button) {
			if (button == 0 && this.contains(mouseX, mouseY)) {
				boolean on = manager().toggleGlobal();
				UiSounds.toggle(on);
				return true;
			}
			return false;
		}
	}
}
