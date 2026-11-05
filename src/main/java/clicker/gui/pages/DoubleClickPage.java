package clicker.gui.pages;

import clicker.clicking.ClickType;
import clicker.config.ClickerConfig;
import clicker.gui.ClickerScreen;
import clicker.gui.Colors;
import clicker.gui.Palette;
import clicker.gui.components.CustomSlider;
import clicker.gui.components.Label;
import clicker.gui.components.RoundedPanel;
import clicker.gui.components.ToggleSwitch;
import clicker.gui.components.UiContext;
import clicker.gui.render.Icon;
import clicker.gui.render.UiRenderer;
import net.minecraft.network.chat.Component;

/** Double clicking: a master switch, an explanation with a live timing diagram, and per-side delays. */
public final class DoubleClickPage extends PageView {
	private static final Component EXPLANATION = Component.translatable("clicker.double.explanation");
	private static final float DIAGRAM_HEIGHT = 40f;

	private final MasterPanel master;
	private final DelayPanel left;
	private final DelayPanel right;

	public DoubleClickPage(ClickerScreen screen) {
		super(screen);
		this.master = this.add(new MasterPanel());
		this.left = this.add(new DelayPanel(ClickType.LEFT));
		this.right = this.add(new DelayPanel(ClickType.RIGHT));
	}

	@Override
	public void layout(UiRenderer r, float x, float y, float width) {
		float masterHeight = this.master.layoutPanel(r, x, y, width);
		float top = y + masterHeight + GAP;
		boolean twoColumns = width >= 300f;
		float col = twoColumns ? (width - GAP) / 2f : width;
		float leftHeight = this.left.layoutPanel(x, top, col);
		if (twoColumns) {
			this.right.layoutPanel(x + col + GAP, top, col);
			this.contentHeight = masterHeight + GAP + leftHeight;
		} else {
			float rightHeight = this.right.layoutPanel(x, top + leftHeight + GAP, col);
			this.contentHeight = masterHeight + GAP + leftHeight + GAP + rightHeight;
		}
	}

	private static ClickType diagramSide() {
		ClickerConfig cfg = config();
		if (!cfg.left.doubleClick && cfg.right.doubleClick) {
			return ClickType.RIGHT;
		}
		return ClickType.LEFT;
	}

	private static final class MasterPanel extends RoundedPanel {
		private final ToggleSwitch toggle;
		private float textHeight;
		private float diagramY;

		MasterPanel() {
			this.header(Icon.DOUBLE, () -> Component.translatable("clicker.double.title"),
					() -> Component.translatable("clicker.double.subtitle"));
			this.toggle = this.add(new ToggleSwitch(() -> config().doubleClickEnabled, value -> {
				config().doubleClickEnabled = value;
				changed();
			}));
		}

		float layoutPanel(UiRenderer r, float x, float y, float w) {
			this.bounds(x, y, w, 0f);
			this.toggle.at(x + w - PADDING - ToggleSwitch.WIDTH, y + PADDING + 2.5f);
			this.textHeight = r.paragraphHeight(EXPLANATION, this.contentWidth(), 2f);
			this.diagramY = this.contentTop() + this.textHeight + 8f;
			float height = this.diagramY + DIAGRAM_HEIGHT + PADDING - y;
			this.bounds(x, y, w, height);
			return height;
		}

		@Override
		protected void drawExtras(UiContext ctx) {
			UiRenderer r = ctx.r;
			Palette p = ctx.p;
			r.paragraph(EXPLANATION, this.contentLeft(), this.contentTop(), this.contentWidth(), p.textSecondary(), 2f);
			this.drawDiagram(r, p, this.contentLeft(), this.diagramY, this.contentWidth(), DIAGRAM_HEIGHT);
		}

		/** A timeline of the clicks that will be sent: tall bars are clicks, short bars are the second clicks. */
		private void drawDiagram(UiRenderer r, Palette p, float x, float y, float w, float h) {
			ClickerConfig cfg = config();
			ClickType type = diagramSide();
			ClickerConfig.SideConfig side = cfg.side(type);
			boolean on = cfg.doubleClickActive(type);
			float interval = 1000f / Math.max(1, side.cps);
			int delay = side.doubleClickDelayMs;
			float window = Math.max(140f, Math.min(1500f, Math.max(interval * 2.6f, delay * 2.4f)));

			r.round(x, y, w, h, 7f, Colors.lerp(p.card(), p.track(), 0.45f));
			float lx = x + 8f;
			float lw = w - 16f;
			float base = y + h - 15f;
			r.round(lx, base, lw, 1.5f, 0.75f, p.track());

			// Tall pills are the scheduled clicks, short ones the second clicks of each pair.
			int primary = side.enabled ? p.accent() : p.textMuted();
			int secondary = Colors.fade(p.accentStrong(), 0.75f);
			int count = 0;
			for (float t = 0f; t <= window && count < 80; t += interval, count++) {
				float px = lx + (t / window) * lw;
				r.round(px - 1.5f, y + 5f, 3f, base - y - 6f, 1.5f, primary);
				r.circle(px, base, 1.6f, primary);
				if (on) {
					float t2 = t + delay;
					if (t2 <= window) {
						float px2 = lx + (t2 / window) * lw;
						r.round(px2 - 1.25f, y + 12f, 2.5f, base - y - 13f, 1.25f, secondary);
						r.circle(px2, base, 1.3f, secondary);
					}
				}
			}

			// Playhead sweeping across the timeline.
			float sweep = (System.nanoTime() / 1_000_000L) % (long) (window * 3f) / (window * 3f);
			float headX = lx + sweep * lw;
			r.round(headX - 0.5f, y + 3f, 1f, base - y - 1f, 0.5f, Colors.fade(p.text(), 0.25f));

			Component legend = on
					? Component.translatable("clicker.double.legend", Component.translatable(type.translationKey()), side.cps, delay)
					: Component.translatable("clicker.double.legend_off", Component.translatable(type.translationKey()), side.cps);
			r.text(r.ellipsize(legend, lw), lx, base + 4f, p.textMuted());
		}
	}

	private static final class DelayPanel extends RoundedPanel {
		private final ClickType type;
		private final ToggleSwitch toggle;
		private final Label delayLabel;
		private final Label delayValue;
		private final CustomSlider slider;
		private final Label hint;

		DelayPanel(ClickType type) {
			this.type = type;
			this.header(type == ClickType.LEFT ? Icon.MOUSE_LEFT : Icon.MOUSE_RIGHT,
					() -> Component.translatable(type.translationKey()),
					() -> Component.translatable("clicker.double.side_subtitle"));
			this.toggle = this.add(new ToggleSwitch(() -> config().side(type).doubleClick, value -> {
				config().side(type).doubleClick = value;
				changed();
			}));
			this.delayLabel = this.add(new Label(() -> Component.translatable("clicker.double.delay"), Palette::textSecondary));
			this.delayValue = this.add(new Label(() -> Component.translatable("clicker.unit.ms_value", config().side(type).doubleClickDelayMs),
					Palette::accentStrong).align(Label.Align.RIGHT));
			this.slider = this.add(new CustomSlider(ClickerConfig.MIN_DOUBLE_CLICK_DELAY_MS, ClickerConfig.MAX_DOUBLE_CLICK_DELAY_MS,
					() -> config().side(type).doubleClickDelayMs,
					value -> {
						config().side(type).doubleClickDelayMs = value;
						changed();
					},
					value -> Component.translatable("clicker.unit.ms_value", value)));
			this.hint = this.add(new Label(() -> Component.translatable("clicker.double.hint", config().side(type).doubleClickDelayMs), Palette::textMuted));
			java.util.function.BooleanSupplier usable = () -> config().doubleClickEnabled;
			java.util.function.BooleanSupplier sideOn = () -> config().doubleClickEnabled && config().side(type).doubleClick;
			this.activeWhen(usable);
			this.slider.activeWhen(sideOn);
			this.delayValue.activeWhen(sideOn);
			this.hint.activeWhen(sideOn);
		}

		float layoutPanel(float x, float y, float w) {
			this.bounds(x, y, w, 0f);
			float cx = this.contentLeft();
			float cw = this.contentWidth();
			this.toggle.at(x + w - PADDING - ToggleSwitch.WIDTH, y + PADDING + 2.5f);
			float top = this.contentTop();
			this.delayLabel.bounds(cx, top, cw * 0.65f, 9f);
			this.delayValue.bounds(cx + cw * 0.65f, top, cw * 0.35f, 9f);
			top += 13f;
			this.slider.bounds(cx, top, cw, CustomSlider.HEIGHT);
			top += CustomSlider.HEIGHT + 3f;
			this.hint.bounds(cx, top, cw, 9f);
			top += 9f + PADDING;
			float height = top - y;
			this.bounds(x, y, w, height);
			return height;
		}
	}
}
