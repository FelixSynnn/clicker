package clicker.gui.pages;

import clicker.ClickerClient;
import clicker.gui.ClickerScreen;
import clicker.gui.ClickerTheme;
import clicker.gui.Palette;
import clicker.gui.components.RoundedPanel;
import clicker.gui.components.ThemeCard;
import clicker.gui.components.UiContext;
import clicker.gui.render.Icon;
import clicker.gui.render.UiRenderer;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Theme presets, shown as live miniature previews. */
public final class ThemesPage extends PageView {
	private final List<ThemeCard> cards = new ArrayList<>();
	private final HintPanel hint;

	public ThemesPage(ClickerScreen screen) {
		super(screen);
		for (ClickerTheme theme : ClickerTheme.PRESETS) {
			this.cards.add(this.add(new ThemeCard(theme,
					() -> ClickerClient.themes().current() == theme,
					() -> ClickerClient.themes().select(theme))));
		}
		this.hint = this.add(new HintPanel());
	}

	@Override
	public void layout(UiRenderer r, float x, float y, float width) {
		int columns = width >= 250f ? 3 : 2;
		float cardW = (width - GAP * (columns - 1)) / columns;
		int rows = (this.cards.size() + columns - 1) / columns;
		for (int i = 0; i < this.cards.size(); i++) {
			int col = i % columns;
			int row = i / columns;
			this.cards.get(i).bounds(x + col * (cardW + GAP), y + row * (ThemeCard.HEIGHT + GAP), cardW, ThemeCard.HEIGHT);
		}
		float gridHeight = rows * ThemeCard.HEIGHT + (rows - 1) * GAP;
		this.hint.bounds(x, y + gridHeight + GAP, width, 24f);
		this.contentHeight = gridHeight + GAP + 24f;
	}

	private static final class HintPanel extends RoundedPanel {
		@Override
		protected void draw(UiContext ctx) {
			UiRenderer r = ctx.r;
			Palette p = ctx.p;
			r.round(this.x, this.y, this.w, this.h, 9f, p.accentSoft());
			r.icon(Icon.SPARKLE, this.x + 9f, this.y + this.h / 2f - 6f, 12f, p.accentStrong());
			Component text = Component.translatable("clicker.themes.hint");
			r.text(r.ellipsize(text, this.w - 36f), this.x + 28f, this.y + this.h / 2f - 4f, p.textSecondary());
		}
	}
}
