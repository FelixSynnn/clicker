package clicker.gui.pages;

import clicker.gui.ClickerScreen;
import clicker.gui.Colors;
import clicker.gui.Palette;
import clicker.gui.components.ItemToggleRow;
import clicker.gui.components.RoundedPanel;
import clicker.gui.components.ToggleSwitch;
import clicker.gui.components.UiContext;
import clicker.gui.render.Icon;
import clicker.gui.render.UiRenderer;
import clicker.protection.ItemProtection;
import clicker.protection.ProtectedCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Right-click item protection: a master switch, the protected item groups and a live "what am I holding" line. */
public final class ProtectionPage extends PageView {
	private static final Component EXPLANATION = Component.translatable("clicker.protection.explanation");

	private final MasterPanel master;
	private final RoundedPanel list;
	private final List<ItemToggleRow> rows = new ArrayList<>();

	public ProtectionPage(ClickerScreen screen) {
		super(screen);
		this.master = this.add(new MasterPanel());
		this.list = this.add(new RoundedPanel());
		this.list.activeWhen(() -> config().protectionEnabled);
		for (ProtectedCategory category : ProtectedCategory.values()) {
			ItemToggleRow row = this.list.add(new ItemToggleRow(category::icon,
					Component.translatable(category.translationKey()),
					Component.translatable(category.descriptionKey()),
					() -> isHeld(category),
					() -> category.isEnabled(config()),
					value -> {
						category.setEnabled(config(), value);
						changed();
					}));
			this.rows.add(row);
		}
	}

	private static boolean isHeld(ProtectedCategory category) {
		ItemProtection.Match match = manager().protectionMatch();
		return match != null && match.category() == category;
	}

	@Override
	public void layout(UiRenderer r, float x, float y, float width) {
		float masterHeight = this.master.layoutPanel(r, x, y, width);
		float top = y + masterHeight + GAP;
		float rowsHeight = this.rows.size() * ItemToggleRow.HEIGHT + (this.rows.size() - 1) * 2f;
		float listHeight = rowsHeight + 12f;
		this.list.bounds(x, top, width, listHeight);
		float rowY = top + 6f;
		for (ItemToggleRow row : this.rows) {
			row.bounds(x + 6f, rowY, width - 12f, ItemToggleRow.HEIGHT);
			rowY += ItemToggleRow.HEIGHT + 2f;
		}
		this.contentHeight = masterHeight + GAP + listHeight;
	}

	private static final class MasterPanel extends RoundedPanel {
		private final ToggleSwitch toggle;
		private float textHeight;

		MasterPanel() {
			this.header(Icon.SHIELD, () -> Component.translatable("clicker.protection.title"),
					() -> Component.translatable("clicker.protection.subtitle"));
			this.toggle = this.add(new ToggleSwitch(() -> config().protectionEnabled, value -> {
				config().protectionEnabled = value;
				changed();
			}));
		}

		float layoutPanel(UiRenderer r, float x, float y, float w) {
			this.bounds(x, y, w, 0f);
			this.toggle.at(x + w - PADDING - ToggleSwitch.WIDTH, y + PADDING + 2.5f);
			this.textHeight = r.paragraphHeight(EXPLANATION, this.contentWidth(), 2f);
			float height = this.contentTop() + this.textHeight + 8f + 18f + PADDING - y;
			this.bounds(x, y, w, height);
			return height;
		}

		@Override
		protected void drawExtras(UiContext ctx) {
			UiRenderer r = ctx.r;
			Palette p = ctx.p;
			r.paragraph(EXPLANATION, this.contentLeft(), this.contentTop(), this.contentWidth(), p.textSecondary(), 2f);

			float sy = this.contentTop() + this.textHeight + 8f;
			float sw = this.contentWidth();
			Player player = Minecraft.getInstance().player;
			ItemProtection.Match match = manager().protectionMatch();
			Component line;
			int color;
			int bg;
			if (!config().protectionEnabled) {
				line = Component.translatable("clicker.protection.live.off");
				color = p.textMuted();
				bg = p.track();
			} else if (player == null) {
				line = Component.translatable("clicker.protection.live.no_world");
				color = p.textMuted();
				bg = Colors.lerp(p.card(), p.track(), 0.6f);
			} else if (match != null) {
				line = Component.translatable("clicker.protection.live.paused", match.stack().getHoverName());
				color = p.warning();
				bg = Colors.fade(p.warning(), 0.14f);
			} else {
				ItemStack main = player.getMainHandItem();
				Component held = main.isEmpty() ? Component.translatable("clicker.protection.live.empty_hand") : main.getHoverName();
				line = Component.translatable("clicker.protection.live.allowed", held);
				color = p.success();
				bg = Colors.fade(p.success(), 0.13f);
			}
			r.round(this.contentLeft(), sy, sw, 18f, 6f, bg);
			r.circle(this.contentLeft() + 9f, sy + 9f, 2.2f, color);
			r.text(r.ellipsize(line, sw - 24f), this.contentLeft() + 16f, sy + 5f, color);
		}
	}
}
