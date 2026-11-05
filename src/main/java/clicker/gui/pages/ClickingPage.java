package clicker.gui.pages;

import clicker.clicking.ActivationMode;
import clicker.clicking.ClickType;
import clicker.config.ClickerConfig;
import clicker.gui.ClickerScreen;
import clicker.gui.Palette;
import clicker.gui.StatusText;
import clicker.gui.components.CustomSlider;
import clicker.gui.components.Label;
import clicker.gui.components.RoundedPanel;
import clicker.gui.components.SegmentedControl;
import clicker.gui.components.ToggleSwitch;
import clicker.gui.components.UiContext;
import clicker.gui.render.Icon;
import clicker.gui.render.UiRenderer;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Left and right clicker settings: enable, CPS (1-100) and activation mode. */
public final class ClickingPage extends PageView {
	private final SidePanel left;
	private final SidePanel right;
	private final TipPanel tip;

	public ClickingPage(ClickerScreen screen) {
		super(screen);
		this.left = this.add(new SidePanel(ClickType.LEFT));
		this.right = this.add(new SidePanel(ClickType.RIGHT));
		this.tip = this.add(new TipPanel());
	}

	@Override
	public void layout(UiRenderer r, float x, float y, float width) {
		boolean twoColumns = width >= 300f;
		float col = twoColumns ? (width - GAP) / 2f : width;
		float leftHeight = this.left.layoutPanel(r, x, y, col);
		float rightHeight;
		float bottom;
		if (twoColumns) {
			rightHeight = this.right.layoutPanel(r, x + col + GAP, y, col);
			float tallest = Math.max(leftHeight, rightHeight);
			this.left.bounds(this.left.x(), this.left.y(), col, tallest);
			this.right.bounds(this.right.x(), this.right.y(), col, tallest);
			bottom = y + tallest;
		} else {
			rightHeight = this.right.layoutPanel(r, x, y + leftHeight + GAP, col);
			bottom = y + leftHeight + GAP + rightHeight;
		}
		float tipHeight = this.tip.layoutTip(r, x, bottom + GAP, width);
		this.contentHeight = bottom + GAP + tipHeight - y;
	}

	private static Component modeDescription(ClickType type) {
		ActivationMode mode = config().side(type).mode;
		return Component.translatable("clicker.clicking.desc." + type.key() + "." + mode.key());
	}

	/** One side's settings card. */
	private static final class SidePanel extends RoundedPanel {
		private final ClickType type;
		private final ToggleSwitch toggle;
		private final Label cpsLabel;
		private final Label cpsValue;
		private final CustomSlider slider;
		private final Label interval;
		private final Label activationLabel;
		private final SegmentedControl<ActivationMode> mode;
		private final Label description;

		SidePanel(ClickType type) {
			this.type = type;
			this.header(type == ClickType.LEFT ? Icon.MOUSE_LEFT : Icon.MOUSE_RIGHT,
					() -> Component.translatable(type.translationKey()),
					() -> Component.translatable("clicker.clicking." + type.key() + ".subtitle"));
			ClickerConfig.SideConfig side = config().side(type);
			this.toggle = this.add(new ToggleSwitch(() -> config().side(type).enabled, value -> {
				config().side(type).enabled = value;
				changed();
			}));
			this.cpsLabel = this.add(new Label(() -> Component.translatable("clicker.clicking.cps"), Palette::textSecondary));
			this.cpsValue = this.add(new Label(() -> Component.translatable("clicker.unit.cps_value", config().side(type).cps),
					Palette::accentStrong).align(Label.Align.RIGHT));
			this.slider = this.add(new CustomSlider(ClickerConfig.MIN_CPS, ClickerConfig.MAX_CPS,
					() -> config().side(type).cps,
					value -> {
						config().side(type).cps = value;
						changed();
					},
					value -> Component.translatable("clicker.unit.cps_value", value)));
			this.interval = this.add(new Label(() -> StatusText.interval(config().side(type).cps), Palette::textMuted));
			this.activationLabel = this.add(new Label(() -> Component.translatable("clicker.clicking.activation"), Palette::textSecondary));
			this.mode = this.add(new SegmentedControl<>(List.of(ActivationMode.TOGGLE, ActivationMode.HOLD),
					m -> Component.translatable(m.translationKey()),
					() -> config().side(type).mode,
					m -> {
						config().side(type).mode = m;
						changed();
					}));
			this.description = this.add(new Label(() -> modeDescription(type), Palette::textMuted).wrap());
			// Controls stay usable but look dimmed while this side is disabled.
			java.util.function.BooleanSupplier enabled = () -> config().side(type).enabled;
			this.slider.activeWhen(enabled);
			this.mode.activeWhen(enabled);
			this.cpsValue.activeWhen(enabled);
			this.interval.activeWhen(enabled);
			this.description.activeWhen(enabled);
			if (side.cps < ClickerConfig.MIN_CPS) {
				side.cps = ClickerConfig.MIN_CPS;
			}
		}

		float layoutPanel(UiRenderer r, float x, float y, float w) {
			this.bounds(x, y, w, 0f);
			float cx = this.contentLeft();
			float cw = this.contentWidth();
			this.toggle.at(x + w - PADDING - ToggleSwitch.WIDTH, y + PADDING + 2.5f);
			float top = this.contentTop();
			this.cpsLabel.bounds(cx, top, cw * 0.7f, 9f);
			this.cpsValue.bounds(cx + cw * 0.7f, top, cw * 0.3f, 9f);
			top += 13f;
			this.slider.bounds(cx, top, cw, CustomSlider.HEIGHT);
			top += CustomSlider.HEIGHT + 3f;
			this.interval.bounds(cx, top, cw, 9f);
			top += 9f + 9f;
			this.activationLabel.bounds(cx, top, cw, 9f);
			top += 13f;
			this.mode.bounds(cx, top, cw, SegmentedControl.HEIGHT);
			top += SegmentedControl.HEIGHT + 7f;
			float descHeight = Math.max(20f, r.paragraphHeight(modeDescription(this.type), cw, 2f));
			// Reserve room for the longer of the two descriptions so the card does not jump when switching modes.
			for (ActivationMode m : ActivationMode.values()) {
				Component text = Component.translatable("clicker.clicking.desc." + this.type.key() + "." + m.key());
				descHeight = Math.max(descHeight, r.paragraphHeight(text, cw, 2f));
			}
			this.description.bounds(cx, top, cw, descHeight);
			top += descHeight + PADDING;
			float height = top - y;
			this.bounds(x, y, w, height);
			return height;
		}
	}

	/** A slim hint bar below the cards. */
	private static final class TipPanel extends RoundedPanel {
		private static final Component TEXT = Component.translatable("clicker.clicking.tip");
		private float textHeight;

		float layoutTip(UiRenderer r, float x, float y, float w) {
			this.textHeight = r.paragraphHeight(TEXT, w - 40f, 2f);
			float height = Math.max(26f, this.textHeight + 16f);
			this.bounds(x, y, w, height);
			return height;
		}

		@Override
		protected void draw(UiContext ctx) {
			UiRenderer r = ctx.r;
			Palette p = ctx.p;
			r.round(this.x, this.y, this.w, this.h, 9f, p.accentSoft());
			r.icon(Icon.INFO, this.x + 10f, this.y + this.h / 2f - 6f, 12f, p.accentStrong());
			r.paragraph(TEXT, this.x + 30f, this.y + (this.h - this.textHeight) / 2f, this.w - 40f, p.textSecondary(), 2f);
		}
	}
}
